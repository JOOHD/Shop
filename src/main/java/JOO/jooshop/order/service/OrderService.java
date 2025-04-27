package JOO.jooshop.order.service;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.global.Exception.MemberNotMatchException;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.order.entity.OrderProduct;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.entity.TemporaryOrderRedis;
import JOO.jooshop.order.model.OrderDto;
import JOO.jooshop.order.repository.OrderRepository;
import JOO.jooshop.order.repository.RedisOrderRepository;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.entity.ProductManagement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;

@Slf4j
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderService {

    /**
     * createOrder는 Redis 저장까지 끝낸다.
     * confirmOrder는 Redis 꺼내서 Orders 저장만 담당.
     * generationMerchantUid()는 주문 고유번호 생성용으로 별도 유지.
     */
    private final RedisOrderRepository redisOrderRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepositoryV1 productRepository;
    private final MemberRepositoryV1 memberRepository;

    /**
     * 장바구니에서 주문 준비
     */
    public Orders createOrder(List<Long> cartIds, OrderDto orderDto) {
        List<Cart> carts = cartRepository.findAllById(cartIds);
        Long memberId = carts.get(0).getMember().getId();
        verifyUserIdMatch(memberId); // 사용자 검증

        // 회원 정보 조회 및 검증
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원 정보를 찾을 수 없습니다."));

        // 장바구니가 동일 회원인지 확인
        boolean sameMember = carts.stream()
                .allMatch(cart -> cart.getMember().getId().equals(memberId));
        if (!sameMember) {
            throw new MemberNotMatchException("주문 생성에 실패했습니다. 회원 정보가 일치하지 않습니다.");
        }

        // 주문 상품 목록 생성
        List<OrderProduct> orderProducts = createOrderProducts(carts);

        // 상품명 목록 및 총 가격 계산
        List<String> productNames = orderProducts.stream()
                .map(OrderProduct::getProductName)
                .collect(Collectors.toList());

        // 총 가격 계산
        BigDecimal totalPrice = orderProducts.stream()
                .map(op -> op.getPriceAtOrder().multiply(BigDecimal.valueOf(op.getQuantity())))  // priceAtOrder 사용
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Orders 객체 생성
        Orders orders = Orders.builder()
                .member(member)
                .phoneNumber(getMemberPhoneNumber(carts))
                .ordererName(member.getUsername())
                .productName(String.join(",", productNames))
                .productManagements(getProductManagements(orderProducts))
                .postCode(orderDto.getPostCode())
                .address(orderDto.getAddress())
                .detailAddress(orderDto.getDetailAddress())
                .merchantUid(orderDto.getMerchantUid())
                .payMethod(orderDto.getPayMethod())
                .totalPrice(totalPrice)
                .build();

        // 임시 주문 정보 Redis에 저장
        saveTemporaryOrder(orderDto, carts);

        return orders;
    }

    private void saveTemporaryOrder(OrderDto orderDto, List<Cart> carts) {
        List<Long> cartIds = carts.stream().map(Cart::getCartId).collect(Collectors.toList());
        List<String> productNames = carts.stream()
                .map(cart -> cart.getProductManagement().getProduct().getProductName())
                .collect(Collectors.toList());

        BigDecimal totalPrice = carts.stream()
                .map(cart -> this.calculateTotalPrice(cart))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TemporaryOrderRedis tempOrder = TemporaryOrderRedis.builder()
                .id("tempOrder:" + orderDto.getMemberId())
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
     * 임시 주문 정보를 기반으로 실제 주문 생성
     */
    public Orders confirmOrder(OrderDto orderDto) {
        String redisKey = "tempOrder:" + orderDto.getMemberId();
        TemporaryOrderRedis temporaryOrderRedis = redisOrderRepository.findById(redisKey)
                .orElseThrow(() -> new IllegalArgumentException("임시 주문 정보를 찾을 수 없습니다."));

        Orders newOrder = Orders.builder()
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

        Orders savedOrder = orderRepository.save(newOrder);
        log.info("주문이 생성되었습니다. 주문 번호: {}", savedOrder.getOrderId());

        return savedOrder;
    }

    /**
     * 주문 상품의 가격과 수량을 기반으로 총 금액을 계산하는 메서드
     *   1. 여기서는 Cart Entity 에 OrderProduct 가 없어서, Management 사용(Cart 와 연관)
     *   2. Management 에 quantity 가 없어서, Cart 를 사용해 가져옴
     *
     *   OrderProduct -> ProductManagement -> Cart
      */
    public BigDecimal calculateTotalPrice(Cart cart) {
        Product product = cart.getProductManagement().getProduct();
        return product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
    }

    /**
     * 주문 번호 생성 (날짜 + UUID)
     */
    private String generationMerchantUid() {
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 장바구니 목록에서 상품명, 가격, 수량 등을 기반으로 OrderProduct 리스트 생성
     */
    private List<OrderProduct> createOrderProducts(List<Cart> carts) {
        List<OrderProduct> orderProducts = new ArrayList<>();
        for (Cart cart : carts) {
            ProductManagement pm = cart.getProductManagement();
            Product product = pm.getProduct();

            OrderProduct orderProduct = OrderProduct.builder()
                    .orders(null)  // 주문이 아직 없으므로 null (나중에 주문이 확정되면 세팅)
                    .productManagement(pm)
                    .productName(product.getProductName())
                    .priceAtOrder((product.getPrice()))
                    .quantity(cart.getQuantity())
                    .build();

            orderProducts.add(orderProduct);
        }
        return orderProducts;
    }

    /**
     * OrderProduct 객체들로부터 ProductManagement만 추출
     */
    private List<ProductManagement> getProductManagements(List<OrderProduct> orderProducts) {
        return orderProducts.stream()
                .map(OrderProduct::getProductManagement)
                .collect(Collectors.toList());
    }

    /**
     * 첫 번째 장바구니의 회원 전화번호 가져오기
     */
    private String getMemberPhoneNumber(List<Cart> carts) {
        Long memberId = carts.get(0).getMember().getId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원 정보를 찾을 수 없습니다."));
        return member.getPhone();
    }
}














