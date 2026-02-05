package JOO.jooshop.thumbnail.repository;

import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductThumbnailRepositoryV1 extends JpaRepository<ProductThumbnail, Long> {

    List<ProductThumbnail> findByProduct_ProductId(Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProductThumbnail t where t.product.productId in :productIds")
    void deleteByProductIds(@Param("ids") List<Long> productIds);

}
