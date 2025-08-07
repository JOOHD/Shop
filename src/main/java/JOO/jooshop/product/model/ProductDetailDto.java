package JOO.jooshop.product.model;

import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.productManagement.entity.enums.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailDto {
    private Long productId;
    private ProductType productType;
    private String productName;
    private BigDecimal price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount;
    private Integer discountRate;
    private Boolean isRecommend;
    private List<String> contentImages; // 썸네일 리스트 추가
    private Size size; // enum -> String 출력 용도
    private String thumbnailUrl; // 대표 이미지용

    // ProductService 클래스에서 modelMapper 를 사용하여 entity -> dto 변환을 구현 가능,
    // 그러나 Product 클래스의 필드와 완전히 일치해야되는 조건을 가진다.
    // 그래서 ProductDetailDto 메서드 주석을 살려 놓은 것 이다.
    public ProductDetailDto(Product product, Size size, String thumbnailUrl) {
        this.productId = product.getProductId();
        this.productType = product.getProductType();
        this.productName = product.getProductName();
        this.price = product.getPrice();
        this.productInfo = product.getProductInfo();
        this.manufacturer = product.getManufacturer();
        this.isDiscount = product.getIsDiscount();
        this.discountRate = product.getDiscountRate();
        this.isRecommend = product.getIsRecommend();
        this.contentImages = product.getContentImages().stream()
                .map(ContentImages::getImagePath)
                .toList();
        this.size = (size != null) ? size : null;
        this.thumbnailUrl = thumbnailUrl;
    }

    // 화면에 출력할 때 사용할 사람이 읽기 좋은 이름 반환 메서드 추가
    public String getSizeDescription() {
        return size != null ? size.getDescription() : "";
    }
}
