package JOO.jooshop.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_color")
@NoArgsConstructor
public class ProductColor {

    @Id
    @Column(name ="color_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long colorId;

    private String color;

    public ProductColor(String color) {
        this.color = color;
    }

    /** ID 기반 생성 */
    public static ProductColor ofId(Long colorId) {
        ProductColor color = new ProductColor();
        color.colorId = colorId;
        return color;
    }

    /** 이름 기반 생성 */
    public static ProductColor ofName(String colorName) {
        ProductColor color = new ProductColor();
        color.color = colorName;
        return color;
    }
}
