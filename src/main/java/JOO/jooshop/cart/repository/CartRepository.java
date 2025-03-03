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
    List<Cart> findByMemberId(Long memberId);

    List<Cart> findByCartIdIn(List<Long> cartIds);

    Cart findByMemberIdAndCartId(Long memberId, Long cartId);

    List<Product> findAllProductByCartIdIn(List<Long> cartIds);

    Cart findByProductManagement(ProductManagement productManagement);

    Optional<Cart> findByProductManagementAndMember(ProductManagement productManagement, Member member);

}
