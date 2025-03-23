package JOO.jooshop.cart.repository;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.productManagement.entity.ProductManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart,Long> {
    // 특정 회원의 장바구니 목록을 조회
    List<Cart> findByMemberId(Long memberId);

    // 여러 장바구니를 한 번에 조회
    List<Cart> findByCartIdIn(List<Long> cartIds);

    // 상품 관리 정보를 기준으로 장바구니를 찾기
    Optional<Cart> findByProductManagement(ProductManagement productManagement);

    // 특정 회원과 특정 상품 관리 정보로 장바구니 조회
    Optional<Cart> findByProductManagementAndMember(ProductManagement productManagement, Member member);

    // 장바구니가 존재하는지 확인하는 메서드
    // boolean existsByProductManagementAndMember(ProductManagement productManagement, Member member);

}
