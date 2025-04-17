package JOO.jooshop.productManagement.model;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
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

    public ProductManagement toEntity(Product product, ProductColor color, Category category, String size) {
        return ProductManagement.builder()
                .product(product)
                .color(color)
                .category(category)
                .size(Size.fromDescription(size))
                .initialStock(initialStock)
                .additionalStock(0L) // 초기 추가재고 없음
                .productStock(initialStock)
                .isRestockAvailable(isRestockAvailable)
                .isRestocked(false) // 재입고 X
                .isSoldOut(false) // 품절 X
                .build();
    }
}
