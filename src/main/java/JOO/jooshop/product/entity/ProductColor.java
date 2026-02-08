// ProductColor.java
package JOO.jooshop.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product_color")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductColor {

    @Id
    @Column(name ="color_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long colorId;

    @Column(nullable = false, length = 50)
    private String color;

    /** 이름 기반 생성 */
    public static ProductColor ofName(String colorName) {
        if (colorName == null || colorName.isBlank()) {
            throw new IllegalArgumentException("colorName must not be blank");
        }
        ProductColor pc = new ProductColor();
        pc.color = colorName.trim();
        return pc;
    }

    /** ID 기반 생성(참조용) */
    public static ProductColor ofId(Long colorId) {
        if (colorId == null) throw new IllegalArgumentException("colorId must not be null");
        ProductColor pc = new ProductColor();
        pc.colorId = colorId;
        return pc;
    }

    // 필요하면 색상명 변경 도메인 메서드만 허용(무분별 @Setter 방지)
    public void rename(String newColorName) {
        if (newColorName == null || newColorName.isBlank()) {
            throw new IllegalArgumentException("newColorName must not be blank");
        }
        this.color = newColorName.trim();
    }
}
