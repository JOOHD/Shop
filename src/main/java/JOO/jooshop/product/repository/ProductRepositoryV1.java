package JOO.jooshop.product.repository;

import JOO.jooshop.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepositoryV1 extends JpaRepository<Product, Long> {

    // fetch join 으로 상품과 썸네일 같이 가져오기
    @EntityGraph(attributePaths = {"productThumbnails"})
    List<Product> findAll();

    // 상품 조회
    Optional<Product> findByProductId(Long productId);

    // 상품 상세 조회 (entity 로 fetch 후 Service 에서 DTO 변환)
    // 연관 엔티티(fetch join)도 같이 가져와 DTO 변환 시 N+1 문제 방지.
    @EntityGraph(attributePaths = {"productThumbnails", "productManagements", "wishLists"})
    Optional<Product> findProductWithDetailsByProductId(Long productId);



}
