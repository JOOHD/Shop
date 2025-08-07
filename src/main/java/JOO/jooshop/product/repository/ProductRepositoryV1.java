package JOO.jooshop.product.repository;

import JOO.jooshop.product.model.ProductDetailDto;
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
    Optional<Product> findByProductId(@Param("id") Long productId);

    // 상품 조회 (view용)
    @Query("SELECT new JOO.jooshop.product.model.ProductDetailDto(p, m.size, t.imagePath) " +
            "FROM Product p " +
            "JOIN p.productManagements m " +  // 다시 JOIN
            "JOIN p.productThumbnails t " +
            "WHERE p.productId = :id")
    List<ProductDetailDto> findProductDetailById(@Param("id") Long productId);


}
