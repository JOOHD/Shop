package JOO.jooshop.productManagement.model;

import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import JOO.jooshop.product.entity.enums.ProductType;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class ProductManagementDto {

    // Product 엔티티의 필드들
    private ProductType productType;
    private String productName;
    private BigDecimal price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount;
    private Boolean isRecommend;

    private Long inventoryId;
    private Long productId;     // Product 테이블의 pk 참조
    private Long colorId;       // ProductColor 테이블의 pk 참조
    private String color;
    private Long categoryId;    // ProductCategory 테이블의 pk 참조
    private String category;
    Size size; //enum
    private Long initialStock;
    private Long additionalStock;
    private Long productStock;
    private boolean isSoldOut;
    private boolean isRestockAvailable;
    private boolean isRestocked;
    
    /* 일반 생성자 */
    public ProductManagementDto(ProductManagement productManagement) {
        this(
                productManagement.getProduct().getProductType(),
                productManagement.getProduct().getProductName(),
                productManagement.getProduct().getPrice(),
                productManagement.getProduct().getProductInfo(),
                productManagement.getProduct().getManufacturer(),
                productManagement.getProduct().getIsDiscount(),
                productManagement.getProduct().getIsRecommend(),
                productManagement.getInventoryId(),
                productManagement.getProduct().getProductId(),
                productManagement.getColor().getColorId(),
                productManagement.getColor().getColor(),
                productManagement.getCategory().getCategoryId(),
                productManagement.getCategory().getName(),
                productManagement.getSize(),
                productManagement.getInitialStock(),
                productManagement.getAdditionalStock(),
                productManagement.getProductStock(),
                productManagement.isSoldOut(),
                productManagement.isRestockAvailable(),
                productManagement.isRestocked()

        );
    }

    /* Entity -> dto */
    public static ProductManagementDto from(ProductManagement productManagement) {
        return ProductManagementDto.builder()
                .productType(productManagement.getProduct().getProductType())
                .productName(productManagement.getProduct().getProductName())
                .price(productManagement.getProduct().getPrice())
                .productInfo(productManagement.getProduct().getProductInfo())
                .manufacturer(productManagement.getProduct().getManufacturer())
                .isDiscount(productManagement.getProduct().getIsDiscount())
                .isRecommend(productManagement.getProduct().getIsRecommend())
                .inventoryId(productManagement.getInventoryId())
                .productId(productManagement.getProduct().getProductId())
                .colorId(productManagement.getColor().getColorId())
                .color(productManagement.getColor().getColor())
                .categoryId(productManagement.getCategory().getCategoryId())
                .category(productManagement.getCategory().getName())
                .size(productManagement.getSize())
                .initialStock(productManagement.getInitialStock())
                .additionalStock(productManagement.getAdditionalStock())
                .productStock(productManagement.getProductStock())
                .isSoldOut(productManagement.isSoldOut())
                .isRestockAvailable(productManagement.isRestockAvailable())
                .isRestocked(productManagement.isRestocked())
                .build();
    }
}
