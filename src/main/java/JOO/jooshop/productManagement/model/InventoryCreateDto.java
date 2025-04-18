package JOO.jooshop.productManagement.model;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryCreateDto {
    @NotNull(message = "상품은 필수로 지정해야 합니다.")
    private Long productId;
    @NotNull(message = "색상은 필수로 지정해야 합니다.")
    private Long colorId;
    @NotNull(message = "카테고리는 필수로 지정해야 합니다.")
    private Long categoryId;
    @NotNull(message = "사이즈는 필수로 지정해야 합니다.")
    private Size size;

    private Long initialStock;
    private Long additionalStock = 0L; // 생성할 땐 무조건 0
//    private Long productStock; // 생성할 땐 productStock = initialStock
    private Boolean isRestockAvailable = false;
    private Boolean isRestocked = false;
    private Boolean isSoldOut = false;

    public InventoryCreateDto(ProductManagement productManagement) {
        this(
                productManagement.getProduct().getProductId(),
                productManagement.getColor().getColorId(),
                productManagement.getCategory().getCategoryId(),
                productManagement.getSize(),
                productManagement.getInitialStock(),
                productManagement.getAdditionalStock(),
                productManagement.isRestockAvailable(),
                productManagement.isRestocked(),
                productManagement.isSoldOut()
        );
    }

    public static ProductManagement newRequestManagementForm(InventoryCreateDto request) {
        Product product = Product.createProductById(request.getProductId());
        ProductColor color = ProductColor.createProductColorById(request.getColorId());
        Category category = Category.createCategoryById(request.getCategoryId());

        return ProductManagement.builder()
                .product(product)
                .color(color)
                .category(category)
                .size(request.getSize())
                .initialStock(request.getInitialStock())
                .productStock(request.getInitialStock()) // 초기재고로 설정
                .isRestockAvailable(request.getIsRestockAvailable())
                .isRestocked(request.getIsRestocked())
                .isSoldOut(request.getIsSoldOut())
                .build();
    }
}
