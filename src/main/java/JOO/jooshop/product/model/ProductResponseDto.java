package JOO.jooshop.product.model;

import JOO.jooshop.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponseDto {
    private Long productId;
    private String productName;
    private Integer price;

    public ProductResponseDto(Product product) {
        this(
                product.getProductId(),
                product.getProductName(),
                product.getPrice()
        );
    }
}
