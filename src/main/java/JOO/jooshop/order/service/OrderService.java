package JOO.jooshop.order.service;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.global.Exception.customException.MemberNotMatchException;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.order.entity.OrderProduct;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.entity.TemporaryOrderRedis;
import JOO.jooshop.order.model.OrderDto;
import JOO.jooshop.order.repository.OrderRepository;
import JOO.jooshop.order.repository.RedisOrderRepository;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.productManagement.entity.ProductManagement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;

@Slf4j
@Service
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderService {

    /**
     * 최종 흐름 요약
     * OrderDto → 프론트에서 입력한 주문자/배송/결제 정보 (front 입력 값)
     * TemporaryOrderRedis → OrderDto + Cart 데이터 합쳐 Redis에 임시 주문 저장 (임시 캐시용 요약본)
     * OrderProduct → Cart 하나씩 꺼내서 주문 상품 단위로 변환 (최종 통합 주문)
     *
     * Orders → OrderDto + TemporaryOrderRedis + OrderProduct 합쳐 DB에 최종 주문 저장
     */
    private final RedisOrderRepository redisOrderRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final MemberRepositoryV1 memberRepository;

    /**
     * ==========================================
     * createOrder
     * ==========================================
     * 역할: 장바구니를 기반으로 임시 주문을 생성하고 Redis에 저장
     *
     * 흐름:
     * 1. cartIds로 Cart 조회
     * 2. 로그인 사용자와 memberId 검증
     * 3. Cart → OrderProduct 변환 (주문 상품 단위)
     * 4. OrderProduct에서 상품명 리스트 및 총 가격 계산
     * 5. OrderDto + Cart 데이터를 TemporaryOrderRedis로 저장 (Redis)
     * 6. Orders 객체를 DB 저장 전 상태로 생성하여 반환
     *
     * 데이터:
     * - OrderDto: 프론트에서 입력된 주문자/배송/결제 정보
     * - Cart: 장바구니 상품 정보
     * - OrderProduct: Cart → 변환된 주문 상품 리스트
     * - TemporaryOrderRedis: Redis에 임시 저장할 요약본
     * - Orders: DB 저장 전 상태 (OrderProduct와 Orders는 아직 연결 X)
     */
    public Orders createOrder(List<Long> cartIds, OrderDto orderDto) {
        // 1. Cart 조회
        var carts = cartRepository.findAllById(cartIds);

        // 2. 사용자 인증
        Long memberId = carts.get(0).getMember().getId();
        verifyUserIdMatch(memberId);
        var member = memberRepository.findById(memberId).orElseThrow();

        // 3. Cart → OrderProduct 변환
        var orderProducts = carts.stream()
                .map(cart -> {
                    var pm = cart.getProductManagement();
                    var product = pm.getProduct();
                    return OrderProduct.builder()
                            .orders(null) // 아직 Orders와 연결되지 않음
                            .productManagement(pm)
                            .productName(product.getProductName())
                            .productSize(pm.getSize() != null ? pm.getSize().name() : null)
                            .productImg(product.getProductThumbnails().isEmpty() ? null
                                    : product.getProductThumbnails().get(0).getImagePath())
                            .priceAtOrder(product.getPrice())
                            .quantity(cart.getQuantity())
                            .build();
                })
                .toList();

        // 4. 상품명 리스트 및 총 가격 계산
        var productNames = orderProducts.stream()
                .map(OrderProduct::getProductName)
                .toList();
        var totalPrice = orderProducts.stream()
                .map(op -> op.getPriceAtOrder().multiply(BigDecimal.valueOf(op.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Redis에 임시 주문 저장
        saveTemporaryOrder(orderDto, carts);

        // 6. DB 저장 전 Orders 객체 생성 (OrderProduct와 연결 전)
        return Orders.builder()
                .member(member)
                .ordererName(member.getUsername())
                .phoneNumber(member.getPhone())
                .productName(String.join(",", productNames))
                .productManagements(orderProducts.stream().map(OrderProduct::getProductManagement).toList())
                .totalPrice(totalPrice)
                .payMethod(orderDto.getPayMethod())
                .postCode(orderDto.getPostCode())
                .address(orderDto.getAddress())
                .detailAddress(orderDto.getDetailAddress())
                .merchantUid(orderDto.getMerchantUid())
                .build();
    }

    /**
     * ==========================================
     * saveTemporaryOrder
     * ==========================================
     * 역할: OrderDto + Cart 데이터를 Redis에 임시 저장
     *
     * 흐름:
     * 1. Cart에서 필요한 정보 추출 (cartIds, productNames, sizes, 이미지)
     * 2. 총 가격 계산
     * 3. TemporaryOrderRedis 객체 생성
     * 4. Redis에 저장
     *
     * 데이터:
     * - 임시 주문 Redis key: "tempOrder:{memberId}"
     * - 임시 주문 객체: 상품 요약 + 사용자 정보 + 총 가격
     */
    private void saveTemporaryOrder(OrderDto orderDto, List<Cart> carts) {

        // 장바구니 id 리스트
        List<Long> cartIds = carts.stream()
                .map(Cart::getCartId)
                .collect(Collectors.toList());

        // 상품명 리스트
        List<String> productNames = carts.stream()
                .map(cart -> cart.getProductManagement().getProduct().getProductName())
                .collect(Collectors.toList());

        // 상품 사이즈 리스트
        List<String> productSizes = carts.stream()
                .map(cart -> cart.getProductManagement().getSize() != null
                        ? cart.getProductManagement().getSize().name() : null)
                .toList();

        // 상품 이미지 리스트
        List<String> productImages = carts.stream()
                .map(cart -> {
                    Product product = cart.getProductManagement().getProduct();
                    return product.getProductThumbnails().isEmpty()
                            ? null
                            : product.getProductThumbnails().get(0).getImagePath();
                })
                .toList();

        // 장바구니 수량
        List<Integer> productQuantities = carts.stream()
                .map(Cart::getQuantity)
                .toList();

        // 총 가격 계산
        BigDecimal totalPrice = carts.stream()
                .map(this::calculateTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 임시 주문 객체 생성
        TemporaryOrderRedis tempOrder = TemporaryOrderRedis.builder()
                .id("tempOrder:" + orderDto.getMemberId())
                .memberId(orderDto.getMemberId())
                .username(orderDto.getOrdererName())
                .phoneNumber(orderDto.getPhoneNumber())
                .cartIds(cartIds)
                .productNames(productNames)
                .productSizes(productSizes)
                .productImages(productImages)
                .productQuantities(productQuantities)
                .totalPrice(totalPrice)
                .build();

        // Redis 저장
        redisOrderRepository.save(tempOrder);
        log.info("임시 주문 정보가 Redis에 저장되었습니다. (Redis 키: {})", tempOrder.getId());
    }

    /**
     * ==========================================
     * confirmOrder
     * ==========================================
     * 역할: 결제 완료 후 Redis 임시 주문 정보를 기반으로
     * Orders와 OrderProduct를 최종 생성하고 DB에 저장
     *
     * 흐름:
     * 1. Redis에서 임시 주문 조회
     * 2. memberId 검증 및 조회
     * 3. 임시 주문의 cartIds로 OrderProduct 생성
     * 4. Orders 객체 생성
     * 5. OrderProduct와 Orders 연결
     * 6. DB에 Orders 저장
     *
     * 데이터:
     * - Redis 임시 주문: TemporaryOrderRedis
     * - Orders: DB 최종 주문
     * - OrderProduct: Orders와 연관된 상품 상세
     */
    public Orders confirmOrder(OrderDto orderDto) {
        // 1. Redis 임시 주문 조회
        var redisKey = "tempOrder:" + orderDto.getMemberId();
        var tempOrder = redisOrderRepository.findById(redisKey)
                .orElseThrow(() -> new IllegalArgumentException("임시 주문 정보가 없습니다."));

        // 2. 회원 조회
        var member = memberRepository.findById(orderDto.getMemberId()).orElseThrow();

        // 3. OrderProduct 리스트 생성
        var orderProducts = tempOrder.getCartIds().stream()
                .map(cartRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(cart -> {
                    var pm = cart.getProductManagement();
                    var product = pm.getProduct();
                    return OrderProduct.builder()
                            .orders(null) // 이후 Orders와 연결
                            .productManagement(pm)
                            .productName(product.getProductName())
                            .productSize(pm.getSize() != null ? pm.getSize().name() : null)
                            .productImg(product.getProductThumbnails().isEmpty() ? null
                                    : product.getProductThumbnails().get(0).getImagePath())
                            .priceAtOrder(product.getPrice())
                            .quantity(cart.getQuantity())
                            .build();
                }).toList();

        // 4. Orders 객체 생성
        var finalOrder = Orders.builder()
                .member(member)
                .ordererName(tempOrder.getUsername())
                .phoneNumber(tempOrder.getPhoneNumber())
                .productName(String.join(",", tempOrder.getProductNames()))
                .productManagements(orderProducts.stream().map(OrderProduct::getProductManagement).toList())
                .totalPrice(tempOrder.getTotalPrice())
                .postCode(orderDto.getPostCode())
                .address(orderDto.getAddress())
                .detailAddress(orderDto.getDetailAddress())
                .payMethod(orderDto.getPayMethod())
                .merchantUid(generationMerchantUid())
                .build();

        // 5. OrderProduct와 Orders 연결
        orderProducts.forEach(op -> op.setOrders(finalOrder));
        finalOrder.setOrderProducts(orderProducts);

        // 6. DB에 저장
        var savedOrder = orderRepository.save(finalOrder);
        log.info("주문 확정 완료: {}", savedOrder.getOrderId());

        return savedOrder;
    }

    /**
     * 장바구니 항목 1건에 대해 상품 가격 * 수량으로 총 가격을 계산합니다.
     */
    public BigDecimal calculateTotalPrice(Cart cart) {
        Product product = cart.getProductManagement().getProduct();
        return product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
    }

    /**
     * 주문 고유 번호(merchantUid)를 날짜 + UUID 기반으로 생성합니다.
     */
    private String generationMerchantUid() {
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}














