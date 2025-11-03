package JOO.jooshop.admin.orders.repository;

import JOO.jooshop.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminOrderRepository extends JpaRepository<Orders, Long> {
    // JpaRepository의 findAll(), findById() 기본 제공
    // 정렬은 Service에서 Sort.by(...) 사용
}

