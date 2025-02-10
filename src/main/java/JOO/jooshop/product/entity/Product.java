package JOO.jooshop.product.entity;

import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.product.model.ProductCreateDto;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import JOO.jooshop.reviewImg.ReviewImg;
import JOO.jooshop.wishList.entity.WishList;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products_table")
public class Product {
//    @SequenceGenerator(
//            name = "product_sequence",
//            sequenceName = "product_sequence",
//            allocationSize = 1
//    )
//    @GeneratedValue(
//            strategy = GenerationType.SEQUENCE,
//            generator = "product_sequence"
//    )
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
    private Integer price;

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
            int price, String productInfo,
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
            int price, String productInfo,
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
    public Product(ProductCreateDto productCreateDto) {
        this.productName = productCreateDto.getProductName();
        this.productType = productCreateDto.getProductType();
        this.price = productCreateDto.getPrice();
        this.productInfo = productCreateDto.getProductInfo();
        this.manufacturer = productCreateDto.getManufacturer();
        this.isDiscount = productCreateDto.getIsDiscount();
        this.discountRate = Boolean.TRUE.equals(productCreateDto.getIsDiscount()) ? productCreateDto.getDiscountRate() : null;
        this.isRecommend = productCreateDto.getIsRecommend();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProduct(ProductCreateDto productCreateDto) {
        this.productName = productCreateDto.getProductName();
        this.productType = productCreateDto.getProductType();
        this.price = productCreateDto.getPrice();
        this.productInfo = productCreateDto.getProductInfo();
        this.manufacturer = productCreateDto.getManufacturer();
        this.isDiscount = productCreateDto.getIsDiscount();
        this.discountRate = Boolean.TRUE.equals(productCreateDto.getIsDiscount()) ? productCreateDto.getDiscountRate() : null;
        this.isRecommend = productCreateDto.getIsRecommend();
        this.updatedAt = LocalDateTime.now();
    }

    public Product(Long productId) {
        this.productId = productId;
    }

    public static Product createProductById(Long productId) {
        return new Product(productId);
    }
}
