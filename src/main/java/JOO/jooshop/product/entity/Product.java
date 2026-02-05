package JOO.jooshop.product.entity;

import JOO.jooshop.admin.products.model.AdminProductRequestDto;
import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.global.time.BaseEntity;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.product.model.ProductRequestDto;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import JOO.jooshop.wishList.entity.WishList;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products_table")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @NotBlank
    @Column(nullable = false)
    private String productName;

    @NotNull
    @Column(nullable = false)
    private BigDecimal price;

    private String productInfo;

    private String manufacturer;

    @Column(nullable = false)
    private boolean isDiscount = false;

    private Integer discountRate;

    @Column(nullable = false)
    private boolean isRecommend = false;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductThumbnail> productThumbnails = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentImages> contentImages = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<ProductManagement> productManagements = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<WishList> wishLists = new ArrayList<>();

    // ⚠️ 파생값이면 제거 권장. 유지한다면 도메인 메서드로만 변경되게 통제 필요
    private Long wishListCount;

    @Column(nullable = false)
    private boolean dummy = false;

    /* =========================
       Factory / Identity
    ========================= */

    public static Product ofId(Long productId) {
        Product p = new Product();
        p.productId = productId;
        return p;
    }

    public static Product createDummy(
            String productName,
            ProductType type,
            BigDecimal price,
            String productInfo,
            String manufacturer,
            Boolean isDiscount,
            Integer discountRate,
            Boolean isRecommend
    ) {
        Product p = new Product();
        p.productName = productName;
        p.productType = type;
        p.price = price;
        p.productInfo = productInfo;
        p.manufacturer = manufacturer;

        boolean discount = Boolean.TRUE.equals(isDiscount);
        p.isDiscount = discount;
        p.discountRate = discount ? discountRate : null;

        p.isRecommend = Boolean.TRUE.equals(isRecommend);
        p.dummy = true;

        return p;
    }

    /* =========================
       Convenience methods
    ========================= */

    /** ✅ 썸네일 추가는 Path 기반으로만 허용(생성 규칙 고정) */
    public void addThumbnailPath(String imagePath) {
        ProductThumbnail thumbnail = ProductThumbnail.create(this, imagePath);
        this.productThumbnails.add(thumbnail);
    }

    public void removeThumbnail(ProductThumbnail thumbnail) {
        if (thumbnail == null) return;
        this.productThumbnails.remove(thumbnail);
    }

    public void addContentImagePath(String imagePath, UploadType uploadType) {
        ContentImages image = ContentImages.create(this, imagePath, uploadType);
        this.contentImages.add(image);
    }

    public void removeContentImage(ContentImages image) {
        if (image == null) return;
        this.contentImages.remove(image);
    }

    /* =========================
       Constructors / Update
    ========================= */

    public Product(AdminProductRequestDto dto) {
        applyAdminDto(dto);
    }

    public Product(ProductRequestDto dto) {
        applyRequestDto(dto);
    }

    public void updateFromRequestDto(ProductRequestDto dto) {
        applyRequestDto(dto);
    }

    public void updateFromDto(AdminProductRequestDto dto) {
        applyAdminDto(dto);
    }

    private void applyRequestDto(ProductRequestDto dto) {
        if (dto == null) throw new IllegalArgumentException("dto must not be null");
        if (dto.getProductName() == null || dto.getProductName().isBlank())
            throw new IllegalArgumentException("productName is required");
        if (dto.getPrice() == null)
            throw new IllegalArgumentException("price is required");

        this.productName = dto.getProductName();
        this.productType = dto.getProductType();
        this.price = dto.getPrice();
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();

        boolean discount = Boolean.TRUE.equals(dto.getIsDiscount());
        this.isDiscount = discount;
        this.discountRate = discount ? dto.getDiscountRate() : null;

        this.isRecommend = Boolean.TRUE.equals(dto.getIsRecommend());
    }

    private void applyAdminDto(AdminProductRequestDto dto) {
        if (dto == null) throw new IllegalArgumentException("dto must not be null");
        if (dto.getProductName() == null || dto.getProductName().isBlank())
            throw new IllegalArgumentException("productName is required");
        if (dto.getPrice() == null)
            throw new IllegalArgumentException("price is required");

        this.productName = dto.getProductName();
        this.productType = dto.getProductType();
        this.price = dto.getPrice();
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();

        boolean discount = Boolean.TRUE.equals(dto.getIsDiscount());
        this.isDiscount = discount;
        this.discountRate = discount ? dto.getDiscountRate() : null;

        this.isRecommend = Boolean.TRUE.equals(dto.getIsRecommend());
    }

    /* =========================
       Helpers
    ========================= */

    public boolean isDummy() {
        return dummy;
    }

    public void markAsReal() {
        this.dummy = false;
    }
}
