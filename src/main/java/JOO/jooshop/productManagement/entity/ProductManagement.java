package JOO.jooshop.productManagement.entity;

import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.product.entity.enums.Gender;
import JOO.jooshop.productManagement.entity.enums.Size;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder(toBuilder = true)
@AllArgsConstructor // 오버로딩 된 생성자 예방
@NoArgsConstructor
@Table(name = "product_management")
public class ProductManagement {
    /*
        inventoryId는 @GeneratedValue 에 의해 자동 생성되기 때문에,
        builder()에는 포함하지 않는 게 자연스럽고 안전.
        
        빌더 패턴 사용
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id" )
    private Long inventoryId; // ProductManagement 테이블의 pk

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "color_id", unique = false, nullable = false)
    private ProductColor color;

    @ManyToOne
    @JoinColumn(name = "category_id", unique = false, nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false)
    private Size size;

    @Column(name = "initial_stock")
    private Long initialStock;

    @Column(name = "additional_stock")
    private Long additionalStock;

    @Column(name = "product_stock")
    private Long productStock;

    // Lombok 빌더에서 초기값 유지하려면 @Builder.Default 필수
    @Builder.Default
    private boolean isSoldOut = false;

    @Builder.Default
    private boolean isRestockAvailable = false;

    @Builder.Default
    private boolean isRestocked = false;

    @Builder.Default
    @ManyToMany(mappedBy = "productManagements")
    private List<Orders> orders = new ArrayList<>();


    public void updateInventory(Category category, Long additionalStock, Long productStock, Boolean isRestockAvailable, Boolean isRestocked, Boolean isSoldOut) {
        this.category = category;
        this.additionalStock = additionalStock;
        this.productStock = productStock;
        this.isRestockAvailable = isRestockAvailable;
        this.isRestocked = isRestocked;
        this.isSoldOut = isSoldOut;
    }
}
