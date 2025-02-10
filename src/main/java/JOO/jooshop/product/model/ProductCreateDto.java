package JOO.jooshop.product.model;

import JOO.jooshop.global.validation.ValidDiscountRate;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidDiscountRate // 커스텀 유효성 검사 애노테이션 적용
public class ProductCreateDto {
    @NotBlank(message = "상품 이름은 필수입니다.")
    private String productName;
    private ProductType productType;
    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Integer price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount = false;
    @Max(value = 100, message = "할인율은 100을 초과할 수 없습니다.")
    private Integer discountRate = null;
    private Boolean isRecommend = false;


    public ProductCreateDto(Product product) {
        this(
                product.getProductName(),
                product.getProductType(),
                product.getPrice(),
                product.getProductInfo(),
                product.getManufacturer(),
                product.getIsDiscount(),
                product.getDiscountRate(),
                product.getIsRecommend()
        );
    }

    // isDiscount가 false 라면 할인율 null
    public void setIsDiscount(Boolean isDiscount) {
        this.isDiscount = isDiscount;
        if (Boolean.FALSE.equals(isDiscount)) {
            this.discountRate = null;
        }
    }


}
