package JOO.jooshop.productManagement.model;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.product.entity.enums.Gender;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import JOO.jooshop.product.entity.enums.ProductType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductManagementDto {

    // Product 엔티티 필드
    private ProductType productType;
    private String productName;
    private String manufacturer;
    private Boolean isDiscount;
    private Boolean isRecommend;

    // ProductManagement 필드
    private Long inventoryId;
    private Long productId;
    private Long colorId;
    private String color;
    private Long categoryId;
    private String category;
    private Gender gender;
    private Size size;
    private Long initialStock;
    private Long additionalStock;
    private Long productStock;
    private boolean isSoldOut;
    private boolean isRestockAvailable;
    private boolean isRestocked;

    /** Entity → DTO */
    public static ProductManagementDto from(ProductManagement pm) {
        return ProductManagementDto.builder()
                .productType(pm.getProduct().getProductType())
                .productName(pm.getProduct().getProductName())
                .manufacturer(pm.getProduct().getManufacturer())
                .isDiscount(pm.getProduct().getIsDiscount())
                .isRecommend(pm.getProduct().getIsRecommend())
                .inventoryId(pm.getInventoryId())
                .productId(pm.getProduct().getProductId())
                .colorId(pm.getColor().getColorId())
                .color(pm.getColor().getColor())
                .categoryId(pm.getCategory().getCategoryId())
                .category(pm.getCategory().getName())
                .gender(pm.getGender())
                .size(pm.getSize())
                .initialStock(pm.getInitialStock())
                .additionalStock(pm.getAdditionalStock())
                .productStock(pm.getProductStock())
                .isSoldOut(pm.isSoldOut())
                .isRestockAvailable(pm.isRestockAvailable())
                .isRestocked(pm.isRestocked())
                .build();
    }

    /** DTO → Entity */
    public ProductManagement toEntity() {
        return ProductManagement.builder()
                .product(Product.builder().productId(this.productId).build())
                .color(ProductColor.ofId(this.colorId))
                .category(Category.ofId(this.categoryId))
                .gender(this.gender)
                .size(this.size)
                .initialStock(this.initialStock)
                .additionalStock(this.additionalStock)
                .productStock(this.productStock)
                .isSoldOut(this.isSoldOut)
                .isRestockAvailable(this.isRestockAvailable)
                .isRestocked(this.isRestocked)
                .build();
    }
}
