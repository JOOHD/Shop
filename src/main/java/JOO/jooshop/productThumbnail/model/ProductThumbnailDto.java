package JOO.jooshop.productThumbnail.model;


import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "thumbnailId")
@AllArgsConstructor
public class ProductThumbnailDto {
    private Long thumbnailId;
    private String imageUrl;
    private Long productId; // Product 테이블의 productId를 참조

    // Entity -> dto 변환
    public ProductThumbnailDto(ProductThumbnail thumbnail) {
        this(
                thumbnail.getThumbnailId(),
                thumbnail.getImagePath(),
                thumbnail.getProduct().getProductId() // thumbnail -> product 참조 객체, 한 번 더 호출
        );
    }
}
