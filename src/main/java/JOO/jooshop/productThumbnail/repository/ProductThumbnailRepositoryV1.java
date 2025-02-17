package JOO.jooshop.productThumbnail.repository;

import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductThumbnailRepositoryV1 extends JpaRepository<ProductThumbnail, Long> {

    List<ProductThumbnail> findByProduct_ProductId(Long productId);

    Optional<ProductThumbnail> findByThumbnailId(Long thumbnailId);
}
