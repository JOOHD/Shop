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
     * 결제 프로세스 흐름 및 주문 저장 방식 설명
     *
     * [1] 사용자가 결제를 진행하면, 프론트에서 createOrder() 호출
     *     - 선택된 장바구니(cartIds)와 배송정보(OrderDto)를 기반으로
     *     - 주문 정보(상품명, 가격, 배송지 등)를 조합하여 Redis에 "임시 주문" 저장
     *     - 실제 DB 저장은 아직 하지 않음 (임시 저장만 수행)
     *
     * [2] 결제가 완료되면, 프론트에서 confirmOrder() 호출
     *     - Iamport(또는 다른 PG사)로부터 결제 성공 콜백 수신 후
     *     - Redis에서 "tempOrder:{memberId}" 키로 임시 주문 정보 조회
     *     - 조회된 정보와 결제 정보(OrderDto)를 조합해 실제 Orders 객체 생성
     *         - 주문자 정보: memberId, 주문자명, 전화번호
     *         - 배송지 정보: 우편번호, 주소, 상세주소
     *         - 상품 정보: 상품명 목록
     *         - 결제 정보: 결제 수단, 총 가격, merchantUid
     *     - DB에 주문 정보 저장 (Orders 테이블)
     *     - 이후 Redis에 저장된 임시 주문 정보는 삭제하거나 유지 여부를 선택 가능
     *
     * [3] 사용자는 '나의 주문 내역' 페이지에서 실제로 저장된 주문을 확인할 수 있음
     */
    private final RedisOrderRepository redisOrderRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final MemberRepositoryV1 memberRepository;

    /**
     * 장바구니를 기반으로 임시 주문을 생성하고 Redis에 저장합니다.
     * 실제 결제 전 단계에서 호출됩니다.
     */
    public Orders createOrder(List<Long> cartIds, OrderDto orderDto) {
        var carts = cartRepository.findAllById(cartIds);
        Long memberId = carts.get(0).getMember().getId();
        verifyUserIdMatch(memberId);
        var member = memberRepository.findById(memberId)
                .orElseThrow();

        var orderProducts = carts.stream()
                .map(cart -> {
                    var pm = cart.getProductManagement();
                    var product = pm.getProduct();
                    return OrderProduct.builder()
                            .orders(null)
                            .productManagement(pm)
                            .productName(product.getProductName())
                            .productSize(pm.getSize() != null
                                                      ? pm.getSize().name() : null)
                            .productImg(product.getProductThumbnails().isEmpty() ? null
                                                      : product.getProductThumbnails().get(0).getImagePath())
                            .priceAtOrder(product.getPrice())
                            .quantity(cart.getQuantity())
                            .build();
                });

        var finalOrder = Orders.builder()
                .member(member)
                .orderName(tempOrder.getUsername)
    }

    /**
     * 임시 주문 정보를 Redis에 저장합니다.
     * 
     * 추가 수정 (25.09.05) -> 임시 주문 내역에 사용자 정보 불러오기
     * TemporaryOrderRedis 클래스에 필요한 사용자 정보 필드 추가
     */
    private void saveTemporaryOrder(OrderDto orderDto, List<Cart> carts) {
        
        // 장바구니 리스트
        List<Long> cartIds = carts.stream()
                .map(Cart::getCartId)
                .collect(Collectors.toList());

        // 상품명
        List<String> productNames = carts.stream()
                .map(cart -> cart.getProductManagement().getProduct().getProductName())
                .collect(Collectors.toList());

        // 상품 사이즈
        List<String> productSizes = carts.stream()
                .map(cart -> cart.getProductManagement().getSize() != null
                        ? cart.getProductManagement().getSize().name() : null)
                .toList();

        // 상품 이미지
        List<String> productImages = carts.stream()
                .map(cart -> {
                            Product product = cart.getProductManagement().getProduct();
                            return product.getProductThumbnails().isEmpty()
                                    ? null
                                    : product.getProductThumbnails().get(0).getImagePath();
                })
                .toList();

        BigDecimal totalPrice = carts.stream()
                .map(this::calculateTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TemporaryOrderRedis tempOrder = TemporaryOrderRedis.builder()
                .id("tempOrder:" + orderDto.getMemberId())
                .memberId(orderDto.getMemberId())
                .username(orderDto.getOrdererName())
                .phoneNumber(orderDto.getPhoneNumber())
                .cartIds(cartIds)
                .productNames(productNames)
                .productSizes(productSizes)   // 객체에서 가져온 사이즈
                .productImages(productImages) // 객체에서 가져온 이미지
                .totalPrice(totalPrice)
                .build();

        redisOrderRepository.save(tempOrder);
        log.info("임시 주문 정보가 Redis에 저장되었습니다. (Redis 키: {})", tempOrder.getId());
    }

    /**
     * Redis에 저장된 임시 주문 정보를 기반으로 최종 주문을 생성하고 DB에 저장합니다.
     * 결제 완료 후 호출됩니다.
     */
    public Orders confirmOrder(OrderDto orderDto) {
        String redisKey = "tempOrder:" + orderDto.getMemberId();

        TemporaryOrderRedis temporaryOrderRedis = redisOrderRepository.findById(redisKey)
                .orElseThrow(() -> new IllegalArgumentException("임시 주문 정보를 찾을 수 없습니다."));

        Orders finalOrder = Orders.builder()
                .member(memberRepository.findById(orderDto.getMemberId()).orElseThrow())
                .postCode(orderDto.getPostCode())
                .address(orderDto.getAddress())
                .detailAddress(orderDto.getDetailAddress())
                .ordererName(orderDto.getOrdererName())
                .phoneNumber(orderDto.getPhoneNumber())
                .payMethod(orderDto.getPayMethod())
                .merchantUid(generationMerchantUid())
                .productName(String.join(",", temporaryOrderRedis.getProductNames()))
                .totalPrice(temporaryOrderRedis.getTotalPrice())
                .build();

        Orders savedOrder = orderRepository.save(finalOrder);
        log.info("주문이 생성되었습니다. 주문 번호: {}", savedOrder.getOrderId());

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

    /**
     * 장바구니 목록을 기반으로 주문 상품 리스트를 생성합니다.
     */
    private List<OrderProduct> createOrderProducts(List<Cart> carts) {
        List<OrderProduct> orderProducts = new ArrayList<>();
        for (Cart cart : carts) {
            ProductManagement pm = cart.getProductManagement();
            Product product = pm.getProduct();

            OrderProduct orderProduct = OrderProduct.builder()
                    .orders(null)
                    .productManagement(pm)
                    .productName(product.getProductName())
                    .priceAtOrder(product.getPrice())
                    .quantity(cart.getQuantity())
                    .build();

            orderProducts.add(orderProduct);
        }
        return orderProducts;
    }

    /**
     * OrderProduct 리스트로부터 ProductManagement 목록을 추출합니다.
     */
    private List<ProductManagement> getProductManagements(List<OrderProduct> orderProducts) {
        return orderProducts.stream()
                .map(OrderProduct::getProductManagement)
                .collect(Collectors.toList());
    }

    /**
     * 장바구니 첫 항목을 기준으로 회원 전화번호를 가져옵니다.
     */
    private String getMemberPhoneNumber(List<Cart> carts) {
        Long memberId = carts.get(0).getMember().getId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원 정보를 찾을 수 없습니다."));
        return member.getPhone();
    }
}














