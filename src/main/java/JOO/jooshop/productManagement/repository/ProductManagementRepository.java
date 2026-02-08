package JOO.jooshop.productManagement.repository;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductManagementRepository extends JpaRepository<ProductManagement, Long> {

    Optional<ProductManagement> findByProductAndColorAndCategoryAndSize(
            Product product, ProductColor color, Category category, Size size
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProductManagement pm where pm.product.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProductManagement pm where pm.product.productId in :productIds")
    void deleteByProductIds(@Param("productIds") List<Long> productIds);
}
