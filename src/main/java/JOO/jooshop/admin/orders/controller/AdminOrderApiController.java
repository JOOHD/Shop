package JOO.jooshop.admin.orders.controller;

import JOO.jooshop.admin.orders.model.AdminOrderResponseDto;
import JOO.jooshop.admin.orders.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderApiController {

    private final AdminOrderService orderService;

    /**
     * 관리자 주문 목록 조회
     * status, keyword는 선택적 필터
     */
    @GetMapping
    public ResponseEntity<List<AdminOrderResponseDto>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        // Service 호출 → DTO 리스트 반환
        List<AdminOrderResponseDto> orders = orderService.getOrders(status, keyword);
        return ResponseEntity.ok(orders);
    }

    /**
     * 관리자 주문 상세 조회
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderResponseDto> getOrderDetail(@PathVariable Long orderId) {

        // Service 호출 → DTO 반환
        AdminOrderResponseDto order = orderService.getOrderDetail(orderId);
        return ResponseEntity.ok(order);
    }
}
