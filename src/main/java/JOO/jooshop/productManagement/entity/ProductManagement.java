package JOO.jooshop.productManagement.entity;

import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.productManagement.entity.enums.Size;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_management")
public class ProductManagement {
    /*@Id
    @SequenceGenerator(
            name = "product_management_sequence",
            sequenceName = "product_management_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "product_management_sequence"
    )*/
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
    @Column(name = "size", nullable = false)
    private Size size;

    @Column(name = "initial_stock")
    private Long initialStock;

    @Column(name = "additional_stock")
    private Long additionalStock;

    @Column(name = "product_stock")
    private Long productStock;

    private boolean isSoldOut = false;

    private boolean isRestockAvailable = false;

    private boolean isRestocked = false;

    @ManyToMany(mappedBy = "productManagements")
    private List<Orders> orders = new ArrayList<>();

    public ProductManagement(Product product, ProductColor color, Category category, Size size, Long initialStock, Long initialStock1, Boolean isRestockAvailable, Boolean isRestocked, Boolean isSoldOut) {
        this.product = product;
        this.color = color;
        this.category = category;
        this.size = size;
        this.initialStock = initialStock;
        this.productStock = initialStock1;
    }

    public ProductManagement(Long initialStock, Long additionalStock, Category categoryById, Product productById, Long productStock, Size size, ProductColor color, boolean isRestockAvailable, boolean isRestocked, boolean isSoldOut) {
        this.initialStock = initialStock;
        this.additionalStock = additionalStock;
        this.category = categoryById;
        this.product = productById;
        this.productStock = productStock;
        this.isRestockAvailable = isRestockAvailable;
        this.isRestocked = isRestocked;
        this.isSoldOut = isSoldOut;
        this.size = size;
        this.color = color;
    }

    public ProductManagement(long initialStock, long additionalStock, Category subCategory, Product product, long productStock, Size size, ProductColor productColor, boolean isRestockAvailable, boolean isRestocked, boolean isSoldOut) {
        this.initialStock = initialStock;
        this.additionalStock = additionalStock;
        this.category = subCategory;
        this.product = product;
        this.productStock = productStock;
        this.isRestockAvailable = isRestockAvailable;
        this.isRestocked = isRestocked;
        this.isSoldOut = isSoldOut;
        this.size = size;
        this.color = productColor;
    }

    public void updateInventory(Category category, Long additionalStock, Long productStock, Boolean isRestockAvailable, Boolean isRestocked, Boolean isSoldOut) {
        this.category = category;
        this.additionalStock = additionalStock;
        this.productStock = productStock;
        this.isRestockAvailable = isRestockAvailable;
        this.isRestocked = isRestocked;
        this.isSoldOut = isSoldOut;
    }
}
