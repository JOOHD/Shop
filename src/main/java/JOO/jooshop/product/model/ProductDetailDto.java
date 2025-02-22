package JOO.jooshop.product.model;

import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailDto {
    private Long productId;
    private ProductType productType;
    private String productName;
    private Integer price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount;
    private Integer discountRate;
    private Boolean isRecommend;
    private List<String> contentImages; // 썸네일 리스트 추가

    // ProductService 클래스에서 modelMapper 를 사용하여 entity -> dto 변환을 구현 가능,
    // 그러나 Product 클래스의 필드와 완전히 일치해야되는 조건을 가진다.
    // 그래서 ProductDetailDto 메서드 주석을 살려 놓은 것 이다.
    public ProductDetailDto(Product product) {
        this(
                product.getProductId(),
                product.getProductType(),
                product.getProductName(),
                product.getPrice(),
                product.getProductInfo(),
                product.getManufacturer(),
                product.getIsDiscount(),
                product.getDiscountRate(),
                product.getIsRecommend(),
                product.getContentImages().stream().map(ContentImages::getImagePath).toList()// 경로만 가져오기

        );
    }

}
