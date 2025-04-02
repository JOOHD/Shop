package JOO.jooshop.productManagement.entity;

import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.productManagement.entity.enums.Size;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_management")
public class ProductManagement {
    /*
        - ProductManagement : 특정 상품에 대한 재고 및 상태를 관리하는 역할입니다.
        - product, color, category, size 를 기준으로
            **구체적인 상품 옵션(variant)**을 관리하고,
            각각의 옵션별 재고(productStock), 품절 상태(isSoldOut),
            재입고 가능 상태(isRestockAvailable), 재입고 여부(isRestocked)를 나타냅니다.
        - Orders 의 @ManyToMany 매핑을 통해 어떤 주문이 이 인벤토리와 연관되는지 표현합니다.
    */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id" )
    private Long inventoryId; // ProductManagement 테이블의 pk

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;  // 관리 대상 상품 (연관 관계: 다대일)

    @ManyToOne
    @JoinColumn(name = "color_id", unique = false, nullable = false)
    private ProductColor color; // 관리 대상 상품 색상 (연관 관계: 다대일)

    @ManyToOne
    @JoinColumn(name = "category_id", unique = false, nullable = false)
    private Category category; // 상품이 속한 카테고리 (연관 관계: 다대일)

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false)
    private Size size; // 상품 사이즈 (Enum 타입으로 관리)

    @Column(name = "initial_stock")
    private Long initialStock; // 최초 입고 수량

    @Column(name = "additional_stock")
    private Long additionalStock; // 추가 입고된 수량

    @Column(name = "product_stock")
    private Long productStock; // 현재 상품 재고 수량

    private boolean isSoldOut = false; // 품절 여부 (true = 품절 상태)

    private boolean isRestockAvailable = false; // 재입고 가능 여부 (true = 재입고 가능)

    private boolean isRestocked = false; // 재입고 상태 여부 (true = 재입고 완료 상태)

    @ManyToMany(mappedBy = "productManagements")
    private List<Orders> orders = new ArrayList<>(); // 주문과의 다대다 연관 관계 (상품 관리 기준으로 매핑)

    // 생성자 1 - 상품 등록 시 필수 값 초기화
    public ProductManagement(Product product, ProductColor color, Category category, Size size, Long initialStock, Long initialStock1, Boolean isRestockAvailable, Boolean isRestocked, Boolean isSoldOut) {
        this.product = product;
        this.color = color;
        this.category = category;
        this.size = size;
        this.initialStock = initialStock;
        this.productStock = initialStock1;
    }

    // 생성자 2 - 추가 필드를 포함한 생성자
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

    // 생성자 3 - 기본 타입으로만 구성된 생성자
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

    /**
     * 재고 정보 업데이트 메서드
     * @param category            카테고리
     * @param additionalStock     추가 입고 수량
     * @param productStock        현재 재고 수량
     * @param isRestockAvailable    재입고 가능 여부
     * @param isRestocked           재입고 여부
     * @param isSoldOut             품절 여부
     */
    public void updateInventory(Category category, Long additionalStock, Long productStock, Boolean isRestockAvailable, Boolean isRestocked, Boolean isSoldOut) {
        this.category = category; // 카테고리 변경
        this.additionalStock = additionalStock; // 추가 재고 변경
        this.productStock = productStock; // 현재 재고 수량 변경
        this.isRestockAvailable = isRestockAvailable; // 재입고 가능 여부 변경
        this.isRestocked = isRestocked; // 재입고 상태 변경
        this.isSoldOut = isSoldOut; // 품절 여부 변경
    }

}
