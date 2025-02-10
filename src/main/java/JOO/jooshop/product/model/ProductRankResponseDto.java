package JOO.jooshop.product.model;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRankResponseDto {
    private Long productId;
    private ProductType productType;
    private String productName;
    private Integer price;
    private Long wishListCount;
    private Boolean isDiscount;
    private Integer discountRate;
    private Boolean isRecommend;
    private String productThumbnails;


    public ProductRankResponseDto(Product product) {
        this(
                product.getProductId(),
                product.getProductType(),
                product.getProductName(),
                product.getPrice(),
                product.getWishListCount(),
                product.getIsDiscount(),
                product.getDiscountRate(),
                product.getIsRecommend(),
                product.getProductThumbnails().get(0).getImagePath()// 경로만 가져오기

        );
    }
}
