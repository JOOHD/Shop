package JOO.jooshop.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "product_color")
public class ProductColor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "color_id")
    private Long colorId;

    @NotBlank(message = "색상 이름은 필수입니다.")
    @Column(name = "color", unique = true)
    private String color;

    public static ProductColor createProductColorById(Long colorId) {
        return new ProductColor(colorId);
    }

    public void setColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            throw new IllegalArgumentException("Color cannot be null or blank");
        }
        this.color = color;
    }

    public ProductColor() {
    }

    public ProductColor(String color) {
        this.color = color;
    }

    public ProductColor(Long colorId) {
        this.colorId = colorId;
    }

}
