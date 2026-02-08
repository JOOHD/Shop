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
@NoArgsConstructor(access = AccessLevel.PROTECTED) // ✅ JPA 기본 생성자만 보호 수준으로
@Table(
        name = "product_management",
        indexes = {
                @Index(name = "idx_pm_product", columnList = "product_id"),
                @Index(name = "idx_pm_category", columnList = "category_id")
        },
        uniqueConstraints = {
                // ✅ "옵션 중복" 원천 차단: 같은 상품에 같은 옵션(성별/사이즈/색/카테고리)이 2개 이상 못 생김
                @UniqueConstraint(
                        name = "uk_pm_option",
                        columnNames = {"product_id", "gender", "size", "color_id", "category_id"}
                )
        }
)
public class ProductManagement {

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

    @Column(name = "is_sold_out", nullable = false)
    private boolean soldOut;

    @Column(name = "is_restock_available", nullable = false)
    private boolean restockAvailable;

    @Column(name = "is_restocked", nullable = false)
    private boolean restocked;

    @ManyToMany(mappedBy = "productManagements")
    private List<Orders> orders = new ArrayList<>();

    /* =========================================================
       Factory (필수값 강제)
    ========================================================= */

    public static ProductManagement create(
            Product product,
            ProductColor color,
            Category category,
            Gender gender,
            Size size,
            long stock
    ) {
        validateRequired(product, color, category, gender, size);
        validateStock(stock);

        ProductManagement pm = new ProductManagement();
        pm.product = product;
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

    /**
     * Dummy 초기화 등에서 사용하던 of(...)는
     * 필수 필드(특히 gender)가 빠져 불완전 객체가 만들어질 수 있어 제거 권장.
     * 그래도 유지하고 싶으면 아래처럼 "필수값 포함" 형태로 변경해야 안전함.
     */
    public static ProductManagement of(
            Product product,
            ProductColor color,
            Category category,
            Gender gender,
            Size size,
            long initialStock,
            Boolean restockAvailable,
            Boolean restocked,
            Boolean soldOut
    ) {
        validateRequired(product, color, category, gender, size);
        validateStock(initialStock);

        ProductManagement pm = new ProductManagement();
        pm.product = product;
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
       Business methods (setter 대신 도메인 메서드)
    ========================================================= */

    public void attachTo(Product product) {
        if (product == null) throw new IllegalArgumentException("product must not be null");
        this.product = product;
    }

    public void detach() {
        this.product = null;
    }

    /**
     * 옵션 메타 변경(카테고리 변경 등)
     * - 옵션 교체는 서비스에서 delete/insert가 최선
     * - 다만 재고 운영 중 카테고리만 바뀌는 케이스가 있다면 허용 가능
     */
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
            Product product,
            ProductColor color,
            Category category,
            Gender gender,
            Size size
    ) {
        if (product == null) throw new IllegalArgumentException("product must not be null");
        if (color == null) throw new IllegalArgumentException("color must not be null");
        if (category == null) throw new IllegalArgumentException("category must not be null");
        if (gender == null) throw new IllegalArgumentException("gender must not be null");
        if (size == null) throw new IllegalArgumentException("size must not be null");
    }

    private static void validateStock(long stock) {
        if (stock < 0) throw new IllegalArgumentException("stock must be >= 0");
    }
}
