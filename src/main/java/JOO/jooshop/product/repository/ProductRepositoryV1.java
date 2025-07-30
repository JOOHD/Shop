package JOO.jooshop.product.repository;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepositoryV1 extends JpaRepository<Product, Long> {

    // fetch join 으로 상품과 썸네일 같이 가져오기
    @EntityGraph(attributePaths = {"productThumbnails"})
    List<Product> findAll();

    // 상품 조회
    Optional<Product> findByProductId(Long productId);
}
