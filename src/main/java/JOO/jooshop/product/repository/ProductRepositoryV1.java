package JOO.jooshop.product.repository;

import org.springframework.data.repository.query.Param;
import JOO.jooshop.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepositoryV1 extends JpaRepository<Product, Long> {

    // fetch join 으로 상품과 썸네일 같이 가져오기
    @EntityGraph(attributePaths = {"productThumbnails"})
    List<Product> findAll();

    // 상품 조회
    @Query("SELECT p FROM Product p JOIN FETCH p.productThumbnails WHERE p.id = :id")
    Optional<Product> findByProductId(@Param("id") Long productId);
}
