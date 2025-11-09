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
        // 전체 주문 조회
        List<Orders> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDay"));

        // 검색,상태 따른 조회
        return orders.stream()
                // status가 null이거나 빈 문자열이면 필터 무시
                .filter(o -> (status == null || status.isBlank())
                        || o.getPaymentStatus().name().equalsIgnoreCase(status))
                // keyword가 null이거나 빈 문자열이면 필터 무시
                .filter(o -> (keyword == null || keyword.isBlank())
                        || o.getOrdererName().contains(keyword)
                        || o.getProductName().contains(keyword))
                .map(AdminOrderResponseDto::toEntity) // DTO -> Entity
                .collect(Collectors.toList());
    }


    /**
     * 주문 상세 조회
     */
    public AdminOrderResponseDto getOrderDetail(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

        return AdminOrderResponseDto.toEntity(order); // DTO -> Entity
    }
}
