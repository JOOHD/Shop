package JOO.jooshop.order.controller;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.model.OrderDto;
import JOO.jooshop.order.model.TempOrderResponse;
import JOO.jooshop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderApiController {

    /*
     * [Controller]

     * 기존
     * - 주문 생성/확정 로직이 단순 요청 처리 형태
     * - 주문 생성 흐름이 명확하게 드러나지 않음
     *
     * refactoring 26.04
     * - 주문 요청 전달 역할만 수행
     * - 비즈니스 로직은 Service로 위임
     */

    private final OrderService orderService;

    @GetMapping("/temp/{memberId}")
    public ResponseEntity<TempOrderResponse> getTemporaryOrder(
            @PathVariable Long memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (!userDetails.getMemberId().equals(memberId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(orderService.getTemporaryOrder(memberId));
    }

    @PostMapping("/create")
    public ResponseEntity<String> createOrder(
            @Valid @RequestBody OrderDto orderDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Orders order = orderService.createOrder(orderDto.getCartIds(), orderDto);
        return ResponseEntity.ok("임시 주문이 Redis에 저장되었습니다.");
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> confirmOrder(
            @RequestBody OrderDto orderDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Orders order = orderService.confirmOrder(orderDto);
        return ResponseEntity.ok("주문이 확정되어 DB에 저장되었습니다. 주문번호: " + order.getOrderId());
    }
}