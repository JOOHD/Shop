package JOO.jooshop.admin.products.repository;

import JOO.jooshop.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminProductRepository extends JpaRepository<Product, Long> {

    /**
     * Admin 상품 목록용
     * - 대표 썸네일 계산을 위해 productThumbnails를 함께 로딩
     * - 목록이므로 옵션(productManagements)까지는 기본으로 끌고 오지 않는 걸 추천
     */
    @EntityGraph(attributePaths = {"productThumbnails"})
    @Query("select p from Product p")
    List<Product> findAllForAdminList();
}
