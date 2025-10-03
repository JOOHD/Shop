package JOO.jooshop.admin.products.dto;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto {

    private Long productId;
    private String productName;
    private ProductType productType;
    private BigDecimal price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount;
    private Integer discountRate;
    private Boolean isRecommend;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Entity → DTO 변환 (정적 팩토리)
     */
    public static ProductDto fromEntity(Product product) {
        return ProductDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productType(product.getProductType())
                .price(product.getPrice())
                .productInfo(product.getProductInfo())
                .manufacturer(product.getManufacturer())
                .isDiscount(product.getIsDiscount())
                .discountRate(product.getDiscountRate())
                .isRecommend(product.getIsRecommend())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /** DTO → Entity (신규 생성용) */
    public Product toEntity() {
        Product product = new Product();
        product.updateFromDto(this);
        return product;
    }
}
