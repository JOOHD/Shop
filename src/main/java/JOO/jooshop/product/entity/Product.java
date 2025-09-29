package JOO.jooshop.product.entity;

import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.product.model.ProductRequestDto;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import JOO.jooshop.wishList.entity.WishList;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products_table")
public class Product {
    /*
        1. 사진을 저장할 엔티티(테이블)을 만든다.
        2. MultipartFile 인터페이스로 구현한다.
        3. 상품과 썸네일은 일대다 (1:N) 관계
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Column(nullable = false, name = "product_name")
    @NotBlank(message = "상품 이름은 필수입니다.")
    private String productName;

    @Column(name = "price", nullable = false, columnDefinition = "INT CHECK (price >= 0)")
    @NotNull(message = "상품 가격은 필수입니다.")
    private BigDecimal price;

    @Column(name = "product_info")
    private String productInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String manufacturer;

    @Column(name = "is_discount", nullable = false)
    private Boolean isDiscount = false;

    @Column(name = "discount_rate", nullable = true)
    private Integer discountRate;

    @Column(name = "is_recommend", nullable = false)
    private Boolean isRecommend = false;

    @OneToMany(mappedBy = "product")
    private List<WishList> wishLists = new ArrayList<>();

    @Column(name = "wishlist_count")
    private Long wishListCount;

    @OneToMany(mappedBy = "product")
    private List<ProductManagement> productManagements = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<PaymentHistory> paymentHistories = new ArrayList<>();

    // CascadeType.ALL 상품 삭제 시, 사진도 삭제 (영속성 전이), orphanRemoval = true (고아 객체 관리)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductThumbnail> productThumbnails = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ContentImages> contentImages = new ArrayList<>();

    public void setWishListCount(Long wishListCount) {
        this.wishListCount = wishListCount;
    }


    public Product() {

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void setLastUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructs a new Product object, for create product dummy data.
     */
    public Product(
            String productName,
            ProductType productType,
            BigDecimal price,
            String productInfo,
            String manufacturer,
            boolean isDiscount,
            Integer discountRate,
            boolean isRecommend) {
        this.productName = productName;
        this.productType = productType;
        this.price = price;
        this.productInfo = productInfo;
        this.manufacturer = manufacturer;
        this.isDiscount = isDiscount;
        this.discountRate = discountRate;
        this.isRecommend = isRecommend;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Product(
            String productName,
            ProductType productType,
            BigDecimal price,
            String productInfo,
            String manufacturer,
            boolean isDiscount,
            boolean isRecommend) {
        this.productName = productName;
        this.productType = productType;
        this.price = price;
        this.productInfo = productInfo;
        this.manufacturer = manufacturer;
        this.isDiscount = isDiscount;
        this.isRecommend = isRecommend;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 상품 생성 메서드에서 사용하는 생성자
    public Product(ProductRequestDto productRequestDto) {
        this.productName = productRequestDto.getProductName();
        this.productType = productRequestDto.getProductType();
        this.price = productRequestDto.getPrice();
        this.productInfo = productRequestDto.getProductInfo();
        this.manufacturer = productRequestDto.getManufacturer();
        this.isDiscount = productRequestDto.getIsDiscount();
        this.discountRate = Boolean.TRUE.equals(productRequestDto.getIsDiscount()) ? productRequestDto.getDiscountRate() : null;
        this.isRecommend = productRequestDto.getIsRecommend();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProduct(ProductRequestDto productRequestDto) {
        this.productName = productRequestDto.getProductName();
        this.productType = productRequestDto.getProductType();
        this.price = productRequestDto.getPrice();
        this.productInfo = productRequestDto.getProductInfo();
        this.manufacturer = productRequestDto.getManufacturer();
        this.isDiscount = productRequestDto.getIsDiscount();
        this.discountRate = Boolean.TRUE.equals(productRequestDto.getIsDiscount()) ? productRequestDto.getDiscountRate() : null;
        this.isRecommend = productRequestDto.getIsRecommend();
        this.updatedAt = LocalDateTime.now();
    }

    public Product(Long productId) {
        this.productId = productId;
    }

    public static Product createProductById(Long productId) {
        return new Product(productId);
    }
}
