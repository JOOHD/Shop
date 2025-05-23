package JOO.jooshop.product.model;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductListDto {
    private Long productId;
    private ProductType productType;
    private String productName;
    private BigDecimal price;
    private LocalDateTime createdAt;
    private Long wishListCount;
    private Boolean isDiscount;
    private Integer discountRate;
    private Boolean isRecommend;
    private List<String> productThumbnails; // 썸네일 리스트 추가

    public ProductListDto(Product product) {
        this(
                product.getProductId(),
                product.getProductType(),
                product.getProductName(),
                product.getPrice(),
                product.getCreatedAt(),
                product.getWishListCount(),
                product.getIsDiscount(),
                product.getDiscountRate(),
                product.getIsRecommend(),
                product.getProductThumbnails().stream().map(ProductThumbnail::getImagePath).toList()// 경로만 가져오기

        );
    }

}
