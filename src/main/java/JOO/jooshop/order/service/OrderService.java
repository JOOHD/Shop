package JOO.jooshop.order.service;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.entity.TemporaryOrderRedis;
import JOO.jooshop.order.entity.enums.PayMethod;
import JOO.jooshop.order.model.OrderDto;
import JOO.jooshop.order.repository.OrderRepository;
import JOO.jooshop.order.repository.RedisOrderRepository;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.entity.ProductManagement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static JOO.jooshop.global.ResponseMessageConstants.MEMBER_NOT_FOUND;
import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class OrderService {
    public final RedisOrderRepository redisOrderRepository;
    public final CartRepository cartRepository;
    public final OrderRepository orderRepository;
    public final ProductRepositoryV1 productRepository;
    public final MemberRepositoryV1 memberRepository;

    public Orders createOrder(List<Long> cartIds, OrderDto orderDto) {
        // 장바구니 조회
        List<Cart> carts = cartRepository.findByCartIdIn(cartIds);

        // 첫 번째 장바구니에서 회원 ID 추출
        Long memberId = carts.get(0).getMember().getId();
        verifyUserIdMatch(memberId);

        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));

        // 상품 목록을 관리하는 리스트
        List<ProductManagement> productMgts = new ArrayList<>();
        for (Cart cart : carts) {
            productMgts.add(cart.getProductManagement());
        }

        // 모든 장바구니의 memberId 가 동일한지 확인
        boolean sameMember = carts.stream()
                .allMatch(cart -> cart.getMember().getId().equals(memberId));
        if (!sameMember || member == null) {
            return null; // 동일한 회원이 아닌 경우 주문 생성 실패
        }

        // 상품명 리스트 생성
        List<String> productNames = getProductNames(carts);
        if (productNames.isEmpty()) {
            throw new IllegalArgumentException("상품명이 비어 있을 수 없습니다.");
        }

        // 주문 객체 생성
        Orders orders = Orders.builder()
                .member(member)
                .productManagements(productMgts)
                .ordererName(member.getUsername()) // order_name
                .productNames(String.join(",", productNames)) // product_names
                .totalPrice(calculateTotalPrice(carts)) // total_price
                .phoneNumber(getMemberPhoneNumber(carts)) // phone_number
                .postCode(orderDto.getPostCode()) // post_code
                .address(orderDto.getAddress()) // address
                .detailAddress(orderDto.getDetailAddress()) // detail_address
                .merchantUid(orderDto.getMerchantUid()) // merchant_uid
                .payMethod(PayMethod.valueOf(orderDto.getPayMethod().toString())) // payMethod enum
                .build();
        return orders;
    }

    // 장바구니 상품명 가져오기
    private List<String> getProductNames(List<Cart> carts) {
        List<String> productNames = new ArrayList<>();
        for (Cart cart : carts) {
            Long productId = cart.getProductManagement().getProduct().getProductId();
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                productNames.add(product.getProductName());
            }
        }
        return productNames;
    }

    // 회원 전화번호 가져오기
    private String getMemberPhoneNumber(List<Cart> carts) {
        Long memberId = carts.get(0).getMember().getId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));
        return member != null && member.getPhone() != null ? member.getPhone() : null;
    }

    // 총 가격 계산
    private BigDecimal calculateTotalPrice(List<Cart> carts) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Cart cart : carts) {
            BigDecimal cartPrice = BigDecimal.valueOf(cart.getPrice());
            totalPrice = totalPrice.add(cartPrice);
        }
        return totalPrice;
    }

    /**
     * 임시 주문 정보 Redis에 저장
     *
     * @param orderDto 주문 DTO
     */
    public void saveTemporaryOrder(OrderDto orderDto) {
        List<Cart> cartList = cartRepository.findByMemberId(orderDto.getMemberId());

        // cartId 목록, 상품명 목록, 총 가격 계산
        List<Long> cartIds = cartList.stream().map(Cart::getCartId).toList();
        List<String> productNames = cartList.stream()
                .map(cart -> cart.getProductManagement().getProduct().getProductName())
                .toList();

        long totalPrice = cartList.stream()
                .mapToLong(cart -> cart.getProductManagement().getProduct().getPrice() * cart.getQuantity())
                .sum();

        // Redis에 저장할 임시 주문 객체 생성
        TemporaryOrderRedis tempOrder = TemporaryOrderRedis.builder()
                .id("tempOrder:" + orderDto.getMemberId()) // Redis 키
                .memberId(orderDto.getMemberId())
                .username(orderDto.getOrdererName())
                .cartIds(cartIds)
                .productNames(productNames)
                .totalPrice(totalPrice)
                .phoneNumber(orderDto.getPhoneNumber())
                .build();

        redisOrderRepository.save(tempOrder);
        log.info("임시 주문 정보가 Redis에 저장되었습니다. (Redis 키: {})", tempOrder.getId());
    }

    /**
     * 주문을 Redis에서 임시 주문 정보를 기반으로 실제 주문으로 저장
     *
     * @param orderDto 주문 DTO
     * @return 실제 주문 객체
     */
    public Orders confirmOrder(OrderDto orderDto) {
        // 1. Redis에서 임시 주문 정보 가져오기
        String redisKey = "tempOrder:" + orderDto.getMemberId();
        TemporaryOrderRedis temporaryOrderRedis = redisOrderRepository.findById(redisKey)
                .orElseThrow(() -> new IllegalArgumentException("임시 주문 정보를 찾을 수 없습니다."));

        // 2. 실제 주문 생성
        Orders newOrder = Orders.builder()
                .member(memberRepository.findById(orderDto.getMemberId()).orElseThrow())
                .postCode(orderDto.getPostCode())
                .address(orderDto.getAddress())
                .detailAddress(orderDto.getDetailAddress())
                .ordererName(orderDto.getOrdererName())
                .phoneNumber(orderDto.getPhoneNumber())
                .payMethod(orderDto.getPayMethod())
                .merchantUid(generationMerchantUid())
                .productNames(String.join(",", temporaryOrderRedis.getProductNames()))
                .totalPrice(BigDecimal.valueOf(temporaryOrderRedis.getTotalPrice()))
                .build();

        // 3. 주문 저장
        Orders savedOrder = orderRepository.save(newOrder);
        log.info("주문이 생성되었습니다. 주문 번호: {}", savedOrder.getOrderId());

        return savedOrder;
    }

    // 주문번호 생성 메서드
    private String generationMerchantUid() {
        String uniqueString = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime today = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDay = today.format(formatter);
        return formattedDay + '-' + uniqueString;
    }
}















