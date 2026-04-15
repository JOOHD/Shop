package JOO.jooshop.productManagement.entity;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.product.entity.enums.Gender;
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
@Table(
        name = "product_management",
        indexes = {
                @Index(name = "idx_pm_product", columnList = "product_id"),
                @Index(name = "idx_pm_category", columnList = "category_id")
        }, // 조회 성능 UP
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pm_option",
                        columnNames = {"product_id", "gender", "size", "color_id", "category_id"}
                )
        }  // 중복 방지
)
public class ProductManagement {

    /*
     * [Entity]
     *
     * 기존
     * - 상품 옵션/재고 엔티티로 사용되었지만
     *   Product와의 관계, 옵션 단위 재고 관리 책임이 코드상 분산될 수 있었음
     *
     * refactoring 26.04
     * - ProductManagement는 상품의 옵션 단위 관리 엔티티
     * - 색상, 사이즈, 카테고리, 재고 등 실제 판매 가능한 SKU 수준의 상태를 관리
     * - Cart / OrderProduct는 Product가 아니라 ProductManagement를 참조하여
     *   "어떤 옵션 상품인지"를 명확히 식별
     * - 상품 본체(Product)와 옵션/재고(ProductManagement)의 책임을 분리
     * - 주문/장바구니에서 옵션 기준 참조를 일관되게 유지하도록 설계
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long inventoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "color_id", nullable = false)
    private ProductColor color;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 20)
    private Size size;

    /**
     * 재고 정책
     * - initialStock: 최초 등록 시 수량(감사용/이력성 의미)
     * - additionalStock: 추가 입고 누적(선택)
     * - productStock: 현재 판매 가능한 재고 (정합성의 기준)
     */
    @Column(name = "initial_stock", nullable = false)
    private long initialStock;

    @Column(name = "additional_stock", nullable = false)
    private long additionalStock;

    @Column(name = "product_stock", nullable = false)
    private long productStock;

    // boolean 3개는 snake_case로 명시
    @Column(name = "is_sold_out", nullable = false)
    private boolean soldOut;

    @Column(name = "is_restock_available", nullable = false)
    private boolean restockAvailable;

    @Column(name = "is_restocked", nullable = false)
    private boolean restocked;

    /**
     * Orders가 연관관계의 주인이라고 가정(mappedBy="productManagements")
     * - 여기서는 "조회용" 컬렉션
     * - add/remove 같은 도메인 조작은 Orders 쪽에서 통일하는 걸 추천
     */
    @ManyToMany(mappedBy = "productManagements")
    private final List<Orders> orders = new ArrayList<>();

    /* =========================================================
       Factory
       - Product와 연결하지 않는다.
       - 생성과 연결 책임 분리
    ========================================================= */

    public static ProductManagement create(
            ProductColor color,
            Category category,
            Gender gender,
            Size size,
            long stock
    ) {
        validateRequired(color, category, gender, size);
        validateStock(stock);

        ProductManagement pm = new ProductManagement();
        pm.color = color;
        pm.category = category;
        pm.gender = gender;
        pm.size = size;

        pm.initialStock = stock;
        pm.additionalStock = 0L;
        pm.productStock = stock;

        pm.restockAvailable = false;
        pm.restocked = false;
        pm.soldOut = (stock == 0);

        return pm;
    }

    public static ProductManagement of(
            ProductColor color,
            Category category,
            Gender gender,
            Size size,
            long initialStock,
            Boolean restockAvailable,
            Boolean restocked,
            Boolean soldOut
    ) {
        validateRequired(color, category, gender, size);
        validateStock(initialStock);

        ProductManagement pm = new ProductManagement();
        pm.color = color;
        pm.category = category;
        pm.gender = gender;
        pm.size = size;

        pm.initialStock = initialStock;
        pm.additionalStock = 0L;
        pm.productStock = initialStock;

        pm.restockAvailable = Boolean.TRUE.equals(restockAvailable);
        pm.restocked = Boolean.TRUE.equals(restocked);
        pm.soldOut = Boolean.TRUE.equals(soldOut) || (initialStock == 0);

        return pm;
    }

    /* =========================================================
       Association (attach / detach)
       - 실제로는 Product가 호출해야 한다.
    ========================================================= */

    public void attachTo(Product product) {
        if (product == null) throw new IllegalArgumentException("product must not be null");
        this.product = product;
    }

    public void detach() {
        this.product = null;
    }

    /* =========================================================
       Business methods (setter 대신 도메인 메서드)
    ========================================================= */

    /**
     * 옵션 메타 변경(카테고리 변경 등)
     * - 옵션 교체는 서비스에서 delete/insert가 최선
     * - 다만 재고 운영 중 카테고리만 바뀌는 케이스가 있다면 허용 가능
     */

    public boolean sameOption(
            ProductColor color,
            Category category,
            Gender gender,
            Size size
    ) {
        return this.color.equals(color)
                && this.category.equals(category)
                && this.gender == gender
                && this.size == size;
    }

    public void changeCategory(Category category) {
        if (category == null) throw new IllegalArgumentException("category must not be null");
        this.category = category;
    }

    /** 입고: 현재 재고 증가 + 추가입고 누적 + 상태 갱신 */
    public void restock(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("restock amount must be positive");

        this.additionalStock += amount;
        this.productStock += amount;
        this.restocked = true;
        this.soldOut = (this.productStock == 0);
    }

    /** 판매/차감: 재고 음수 방지 */
    public void decreaseStock(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("decrease amount must be positive");
        if (this.productStock < amount) throw new IllegalStateException("insufficient stock");

        this.productStock -= amount;
        this.soldOut = (this.productStock == 0);
    }

    /** 재고 수동 보정(관리자용) */
    public void adjustStock(long newStock) {
        if (newStock < 0) throw new IllegalArgumentException("stock must be >= 0");
        this.productStock = newStock;
        this.soldOut = (this.productStock == 0);
    }

    public void setRestockAvailable(boolean available) {
        this.restockAvailable = available;
    }

    /* =========================================================
       Validation
    ========================================================= */

    private static void validateRequired(
            ProductColor color,
            Category category,
            Gender gender,
            Size size
    ) {
        if (color == null) throw new IllegalArgumentException("color must not be null");
        if (category == null) throw new IllegalArgumentException("category must not be null");
        if (gender == null) throw new IllegalArgumentException("gender must not be null");
        if (size == null) throw new IllegalArgumentException("size must not be null");
    }

    private static void validateStock(long stock) {
        if (stock < 0) throw new IllegalArgumentException("stock must be >= 0");
    }
}
