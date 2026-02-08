package JOO.jooshop.product.repository;

import JOO.jooshop.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // fetch join 으로 상품과 썸네일 같이 가져오기
    @EntityGraph(attributePaths = {"productThumbnails"})
    List<Product> findAll();

    // 상품 조회
    Optional<Product> findByProductId(Long productId);

    @Query("select p.idform Product p where p.dummy = true")
    List<Long> findDummyIds();

    // ❌ deleteByIdIn(List<Long>)  -> Product에 id 필드가 없어서 파생쿼리로 만들면 깨질 수 있음
    // ✅ Spring Data JPA 기본 제공 메서드로 대체해서 사용 추천:
    void deleteByProductId(List<Long> productIds);

    // 상품 상세 조회 (entity 로 fetch 후 Service 에서 DTO 변환)
    // 연관 엔티티(fetch join)도 같이 가져와 DTO 변환 시 N+1 문제 방지.
    @EntityGraph(attributePaths = {"productThumbnails", "productManagements", "wishLists"})
    Optional<Product> findProductWithDetailsByProductId(Long productId);



}
