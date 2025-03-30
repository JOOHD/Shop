package JOO.jooshop.global.dummydata.util;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import JOO.jooshop.productThumbnail.repository.ProductThumbnailRepositoryV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;


/**
 * Utility class for generating and saving product thumbnail data.
 */
@Component
@RequiredArgsConstructor
public class ProductThumbnailDataUtil {

    private final ProductRepositoryV1 productRepository;
    private final ProductThumbnailRepositoryV1 productThumbnailRepository;

    public void generateProductThumbnailDataV1(int count) {
        String[] images = {
                "accessories_bag_1.jpg", "outer_cardigan_2.jpg", "shoes_sneakers_1.jpg",
                "accessories_bag_2.jpg", "outer_coat_1.jpg", "shoes_sneakers_2.jpg",
                "accessories_cap_1.jpg", "outer_coat_2.jpg", "top_blouse_1.jpg",
                "accessories_cap_2.jpg", "outer_jacket_1.jpg", "top_blouse_2.jpg",
                "accessories_socks_1.jpg", "outer_jacket_2.jpg", "top_hoodie_1.jpg",
                "accessories_socks_2.jpg", "outer_lightweight-padding_1.jpg","top_hoodie_2.jpg",
                "bottom_long_1.jpg", "outer_lightweight-padding_2.jpg", "top_knit-sweater_1.jpg",
                "bottom_long_2.jpg", "outer_long-padding_1.jpg", "top_knit-sweater_2.jpg",
                "bottom_shorts_1.jpg", "outer_long-padding_2.jpg", "top_long-shirts_1.jpg",
                "bottom_shorts_2.jpg", "outer_mustang_1.jpg", "top_long-shirts_2.jpg",
                "bottom_skirt_1.jpg", "outer_mustang_2.jpg", "top_long-sleeve_1.jpg",
                "bottom_skirt_2.jpg", "outer_short-padding_1.jpg", "top_long-sleeve_2.jpg",
                "dress&set_dress_1.jpg", "outer_short-padding_2.jpg", "top_short-shirts_1.jpg",
                "dress&set_dress_2.jpg", "outer_vest_1.jpg", "top_short-shirts_2.jpg",
                "dress&set_set-up_1.jpg", "outer_vest_2.jpg", "top_short-sleeve_1.jpg",
                "dress&set_set-up_2.jpg", "shoes_boots_1.jpg", "top_short-sleeve_2.jpg",
                "dress&set_two-piece_1.jpg", "shoes_boots_2.jpg", "top_sweatshirt_1.jpg",
                "dress&set_two-piece_2.jpg", "shoes_sandal_1.jpg", "top_sweatshirt_2.jpg",
                "outer_cardigan_1.jpg", "shoes_sandal_2.jpg"
        };

        IntStream.rangeClosed(1, count).forEach(i -> {
            // Get image file name from array
            String imageFile = images[i % images.length];

            // Split file name into parts
            String[] parts = imageFile.split("_");

            // 이미지 경로 생성
            String imagePath = String.format("/static/uploads/thumbnails/%s", imageFile);

            // 상품 조회
            Product product = productRepository.findById((long) i)
                    .orElseThrow(() -> new NoSuchElementException("해당 상품이 존재하지 않습니다."));

            // 썸네일 생성 및 저장
            ProductThumbnail productThumbnail = new ProductThumbnail(product, imagePath);
            productThumbnailRepository.save(productThumbnail);
        });
    }

    public void generateProductThumbnailDataV2(List<String> imagePaths) {
        IntStream.range(0, imagePaths.size()).forEach(i -> {
            // 이미지 경로 생성
            String imagePath = String.format("/uploads/thumbnails/%s", imagePaths.get(i));

            // 상품 조회 - 필요에 따라 생성된 상품의 식별자를 알고 있는 경우 수정 가능
            Product product = productRepository.findById((long) (i+1))
                    .orElseThrow(() -> new NoSuchElementException("해당 상품이 존재하지 않습니다."));

            // 썸네일 생성 및 저장
            ProductThumbnail productThumbnail = new ProductThumbnail(product, imagePath);
            productThumbnailRepository.save(productThumbnail);
        });
    }

    public void generateProductThumbnailDataV3(List<Product> products, List<String> imagePaths) {
        if(imagePaths.size() != products.size()) {
            throw new IllegalArgumentException("The number of products and image paths should be equal");
        }

        for (int i = 0; i < products.size(); i++) {
            // 이미지 경로 생성
            String imagePath = String.format("/uploads/thumbnails/%s", imagePaths.get(i));
            // 상품 추출
            Product product = products.get(i);
            // 썸네일 생성 및 저장
            ProductThumbnail productThumbnail = new ProductThumbnail(product, imagePath);
            productThumbnailRepository.save(productThumbnail);
        }
    }
}
