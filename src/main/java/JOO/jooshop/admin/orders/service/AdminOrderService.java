package JOO.jooshop.admin.orders.service;

import JOO.jooshop.admin.orders.model.AdminOrderResponseDto;
import JOO.jooshop.admin.orders.repository.AdminOrderRepository;
import JOO.jooshop.order.entity.Orders;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final AdminOrderRepository orderRepository;

    /**
     * 관리자용 주문 목록 조회
     */
    public List<AdminOrderResponseDto> getOrders(String status, String keyword) {
        List<Orders> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDay"));

        return orders.stream()
                .filter(o -> status == null || o.getPaymentStatus().name().equalsIgnoreCase(status))
                .filter(o -> keyword == null || o.getOrdererName().contains(keyword)
                                                    || o.getProductName().contains(keyword))
                .map(AdminOrderResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 주문 상세 조회
     */
    public AdminOrderResponseDto getOrderDetail(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

        return AdminOrderResponseDto.fromEntity(order);
    }
}
