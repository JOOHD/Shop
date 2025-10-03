package JOO.jooshop.admin.products.dto;

import JOO.jooshop.product.entity.enums.ProductType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductCreateDto {
    private String productName;
    private ProductType productType;
    private BigDecimal price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount;
    private Integer discountRate;
    private Boolean isRecommend;
}
