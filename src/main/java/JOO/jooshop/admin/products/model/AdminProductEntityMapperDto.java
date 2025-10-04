package JOO.jooshop.admin.products.model;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.product.model.ProductRequestDto;
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
public class AdminProductEntityMapperDto {

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
    public static AdminProductEntityMapperDto fromEntity(Product product) {
        return AdminProductEntityMapperDto.builder()
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

    /**
     *  일반 상품(ProductRequestDto) → AdminProductEntityMapperDto 변환 생성자
     *  (ProductServiceV1.updateProduct 에서 사용됨)
     */
    public AdminProductEntityMapperDto(ProductRequestDto dto) {
        this.productName = dto.getProductName();
        this.productType = dto.getProductType();
        this.price = dto.getPrice();
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();
        this.isDiscount = dto.getIsDiscount();
        this.discountRate = dto.getDiscountRate();
        this.isRecommend = dto.getIsRecommend();
    }

    /** DTO → Entity (신규 생성용) */
    public Product toEntity() {
        Product product = new Product();
        product.updateFromDto(this);
        return product;
    }
}
