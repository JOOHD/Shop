package JOO.jooshop.global.dummydata.util;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import JOO.jooshop.thumbnail.repository.ProductThumbnailRepositoryV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class ProductThumbnailDataUtil {

    private final ProductRepositoryV1 productRepository;
    private final ProductThumbnailRepositoryV1 productThumbnailRepository;

    public void generateProductThumbnailDataV2(List<String> imagePaths) {
        IntStream.range(0, imagePaths.size()).forEach(i -> {
            String imagePath = String.format("/uploads/thumbnails/%s", imagePaths.get(i));

            Product product = productRepository.findById((long) (i + 1))
                    .orElseThrow(() -> new NoSuchElementException("해당 상품이 존재하지 않습니다."));

            ProductThumbnail thumbnail = new ProductThumbnail(product, imagePath);
            productThumbnailRepository.save(thumbnail);
        });
    }
}
