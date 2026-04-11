package JOO.jooshop.cart.repository;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.productManagement.entity.ProductManagement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * 특정 회원의 장바구니 전체 조회
     */
    @EntityGraph(attributePaths = {"productManagement", "productManagement.product"})
    List<Cart> findByMemberId(Long memberId);

    /**
     * 특정 회원이 특정 상품 옵션을 담은 기존 장바구니 조회
     */
    Optional<Cart> findByMemberAndProductManagement(Member member, ProductManagement productManagement);

    /**
     * 여러 장바구니 ID를 한 번에 조회
     */
    List<Cart> findByCartIdIn(List<Long> cartIds);

    /**
     * 여러 장바구니 ID 중 특정 회원 소유 장바구니만 조회
     */
    List<Cart> findByCartIdInAndMemberId(List<Long> cartIds, Long memberId);
}