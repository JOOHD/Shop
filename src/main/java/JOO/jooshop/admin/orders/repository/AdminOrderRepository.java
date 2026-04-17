package JOO.jooshop.admin.orders.repository;

import JOO.jooshop.order.entity.Orders;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminOrderRepository extends JpaRepository<Orders, Long> {

    /**
     * 관리자 주문 목록 조회
     */
    List<Orders> findAll(Sort sort);

    /**
     * 관리자 주문 상세 조회
     * - 상세에서는 orderProducts까지 함께 로딩
     * - 이건 상세에서 orderProducts를 쓸 게 확실하니까
         조회 시 같이 가져오는 구조로 잡음
     */
    @EntityGraph(attributePaths = {"orderProducts"})
    Optional<Orders> findWithOrderProductsByOrderId(Long orderId);
}