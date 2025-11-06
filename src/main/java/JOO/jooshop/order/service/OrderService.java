package JOO.jooshop.order.service;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.order.entity.OrderProduct;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.entity.TemporaryOrderRedis;
import JOO.jooshop.order.model.OrderDto;
import JOO.jooshop.order.repository.OrderRepository;
import JOO.jooshop.order.repository.RedisOrderRepository;
import JOO.jooshop.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;

@Slf4j
@Service
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderService {

    private final RedisOrderRepository redisOrderRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final MemberRepositoryV1 memberRepository;

    /**
     * Redis 임시 주문 저장 + DB Orders 객체 생성 (OrderProduct와 연결 전)
     */
    public Orders createOrder(List<Long> cartIds, OrderDto orderDto) {
        var carts = cartRepository.findAllById(cartIds);
        Long memberId = carts.get(0).getMember().getId();
        verifyUserIdMatch(memberId);
        var member = memberRepository.findById(memberId).orElseThrow();

        // Cart → OrderProduct 변환 (Orders 연결 전이므로 null)
        var orderProducts = carts.stream()
                .map(cart -> {
                    var pm = cart.getProductManagement();
                    var product = pm.getProduct();
                    return OrderProduct.createOrderProduct(
                            null, // Orders 아직 연결 전
                            pm,
                            product.getProductName(),
                            pm.getSize() != null ? pm.getSize().name() : null,
                            product.getProductThumbnails().isEmpty() ? null : product.getProductThumbnails().get(0).getImagePath(),
                            product.getPrice(),
                            cart.getQuantity()
                    );
                })
                .toList();

        // 총 가격 계산
        BigDecimal totalPrice = orderProducts.stream()
                .map(op -> op.getPriceAtOrder().multiply(BigDecimal.valueOf(op.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Redis 임시 주문 저장
        saveTemporaryOrder(orderDto, carts, totalPrice);

        // Orders 객체 생성 (OrderProduct 연결 전)
        return Orders.createOrder(
                member,
                member.getUsername(),
                member.getPhoneNumber(),
                orderProducts.stream().map(OrderProduct::getProductName).collect(Collectors.joining(",")),
                totalPrice,
                orderDto.getPostCode(),
                orderDto.getAddress(),
                orderDto.getDetailAddress(),
                orderDto.getPayMethod(),
                orderDto.getMerchantUid()
        );
    }

    /** Redis 임시 주문 저장 */
    private void saveTemporaryOrder(OrderDto orderDto, List<Cart> carts, BigDecimal totalPrice) {
        TemporaryOrderRedis tempOrder = TemporaryOrderRedis.builder()
                .id("tempOrder:" + orderDto.getMemberId())
                .memberId(orderDto.getMemberId())
                .username(orderDto.getUsername())
                .ordererName(orderDto.getOrdererName())
                .phoneNumber(orderDto.getPhoneNumber())
                .cartIds(carts.stream().map(Cart::getCartId).toList())
                .productNames(carts.stream().map(c -> c.getProductManagement().getProduct().getProductName()).toList())
                .productSizes(carts.stream().map(c -> c.getProductManagement().getSize() != null
                        ? c.getProductManagement().getSize().name() : null).toList())
                .productImages(carts.stream().map(c -> {
                    Product product = c.getProductManagement().getProduct();
                    return product.getProductThumbnails().isEmpty() ? null
                            : product.getProductThumbnails().get(0).getImagePath();
                }).toList())
                .productQuantities(carts.stream().map(Cart::getQuantity).toList())
                .totalPrice(totalPrice)
                .build();

        redisOrderRepository.save(tempOrder);
        log.info("임시 주문 Redis 저장: {}", tempOrder.getId());
    }

    /** 결제 완료 후 Redis 기반 Orders/OrderProduct 최종 생성 및 저장 */
    public Orders confirmOrder(OrderDto orderDto) {
        var tempOrder = redisOrderRepository.findById("tempOrder:" + orderDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("임시 주문 정보 없음"));

        var member = memberRepository.findById(orderDto.getMemberId()).orElseThrow();

        // Cart → OrderProduct 변환
        var orderProducts = tempOrder.getCartIds().stream()
                .map(cartRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(cart -> {
                    var pm = cart.getProductManagement();
                    var product = pm.getProduct();
                    return OrderProduct.createOrderProduct(
                            null, // Orders 아직 연결 전
                            pm,
                            product.getProductName(),
                            pm.getSize() != null ? pm.getSize().name() : null,
                            product.getProductThumbnails().isEmpty() ? null : product.getProductThumbnails().get(0).getImagePath(),
                            product.getPrice(),
                            cart.getQuantity()
                    );
                })
                .toList();

        // Orders 객체 생성
        var finalOrder = Orders.createOrder(
                member,
                tempOrder.getUsername(),
                tempOrder.getPhoneNumber(),
                String.join(",", tempOrder.getProductNames()),
                tempOrder.getTotalPrice(),
                orderDto.getPostCode(),
                orderDto.getAddress(),
                orderDto.getDetailAddress(),
                orderDto.getPayMethod(),
                generationMerchantUid()
        );

        finalOrder.addOrderProducts(orderProducts);

        var savedOrder = orderRepository.save(finalOrder);
        log.info("주문 확정 완료: {}", savedOrder.getOrderId());

        return savedOrder;
    }

    /** 장바구니 1건 총 가격 계산
    public BigDecimal calculateTotalPrice(Cart cart) {
        return cart.getProductManagement().getProduct().getPrice()
                .multiply(BigDecimal.valueOf(cart.getQuantity()));
    }*/

    /** 주문 고유 번호 생성 */
    private String generationMerchantUid() {
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
