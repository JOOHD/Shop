package JOO.jooshop.thumbnail.repository;

import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductThumbnailRepositoryV1 extends JpaRepository<ProductThumbnail, Long> {

    List<ProductThumbnail> findByProductProductId(Long productId);

    Optional<ProductThumbnail> findByThumbnailId(Long thumbnailId);
}
