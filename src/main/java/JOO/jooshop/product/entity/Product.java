package JOO.jooshop.product.entity;

import JOO.jooshop.admin.products.model.AdminProductRequestDto;
import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.global.time.BaseEntity;
import JOO.jooshop.product.entity.enums.Gender;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.product.model.ProductRequestDto;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import JOO.jooshop.wishList.entity.WishList;
import JOO.jooshop.categorys.entity.Category;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products_table")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    /** Product가 생명주기 완전 소유 */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ProductThumbnail> productThumbnails = new ArrayList<>();

    /** Product가 생명주기 완전 소유 */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ContentImages> contentImages = new ArrayList<>();

    /** Product가 생명주기 완전 소유 */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ProductManagement> productManagements = new ArrayList<>();

    /** 사용자 활동 데이터 - Product aggregate 외부 */
    @OneToMany(mappedBy = "product")
    private final List<WishList> wishLists = new ArrayList<>();

    private Long wishListCount;

    @Column(nullable = false)
    private boolean dummy = false;

    /* =========================
       Factory
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
        p.productName = requireText(productName, "productName");
        p.productType = requireNotNull(type, "productType");
        p.price = requireNotNull(price, "price");
        p.productInfo = productInfo;
        p.manufacturer = manufacturer;

        p.applyDiscount(Boolean.TRUE.equals(isDiscount), discountRate);
        p.isRecommend = Boolean.TRUE.equals(isRecommend);
        p.dummy = true;
        return p;
    }

    public static Product create(
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
        p.productName = requireText(productName, "productName");
        p.productType = requireNotNull(type, "productType");
        p.price = requireNotNull(price, "price");
        p.productInfo = productInfo;
        p.manufacturer = manufacturer;

        p.applyDiscount(Boolean.TRUE.equals(isDiscount), discountRate);
        p.isRecommend = Boolean.TRUE.equals(isRecommend);
        p.dummy = false;
        return p;
    }

    /* =========================
       Read-only views
    ========================= */

    public List<ProductThumbnail> thumbnailsView() {
        return Collections.unmodifiableList(productThumbnails);
    }

    public List<ContentImages> contentImagesView() {
        return Collections.unmodifiableList(contentImages);
    }

    public List<ProductManagement> optionsView() {
        return Collections.unmodifiableList(productManagements);
    }

    /* =========================
       Aggregate behaviors
       - 자식 추가/삭제/연결은 Product만 책임진다.
    ========================= */

    public void addThumbnailPath(String imagePath) {
        String path = requireText(imagePath, "imagePath");

        ProductThumbnail thumbnail = ProductThumbnail.create(path);
        thumbnail.attachTo(this);

        this.productThumbnails.add(thumbnail);
    }

    public void removeThumbnail(ProductThumbnail thumbnail) {
        if (thumbnail == null) {
            return;
        }

        if (this.productThumbnails.remove(thumbnail)) {
            thumbnail.detach();
        }
    }

    public void addContentImagePath(String imagePath, UploadType uploadType) {
        ContentImages image = ContentImages.create(this, imagePath, uploadType);
        this.contentImages.add(image);
    }

    public void removeContentImage(ContentImages image) {
        if (image == null) {
            return;
        }

        if (this.contentImages.remove(image)) {
            image.detach();
        }
    }

    public void addOption(
            ProductColor color,
            Category category,
            Gender gender,
            Size size,
            long stock
    ) {
        validateDuplicateOption(color, category, gender, size);

        ProductManagement option = ProductManagement.create(
                color,
                category,
                gender,
                size,
                stock
        );
        option.attachTo(this);

        this.productManagements.add(option);
    }

    public void addProductManagement(ProductManagement pm) {
        if (pm == null) {
            throw new IllegalArgumentException("productManagement must not be null");
        }

        validateDuplicateOption(
                pm.getColor(),
                pm.getCategory(),
                pm.getGender(),
                pm.getSize()
        );

        pm.attachTo(this);
        this.productManagements.add(pm);
    }

    public void removeProductManagement(ProductManagement pm) {
        if (pm == null) {
            return;
        }

        if (this.productManagements.remove(pm)) {
            pm.detach();
        }
    }

    private void validateDuplicateOption(
            ProductColor color,
            Category category,
            Gender gender,
            Size size
    ) {
        boolean duplicated = this.productManagements.stream()
                .anyMatch(pm ->
                        pm.sameOption(color, category, gender, size)
                );

        if (duplicated) {
            throw new IllegalStateException("already exists same option in product");
        }
    }

    /* =========================
       Update
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
        if (dto == null) {
            throw new IllegalArgumentException("dto must not be null");
        }

        this.productName = requireText(dto.getProductName(), "productName");
        this.productType = requireNotNull(dto.getProductType(), "productType");
        this.price = requireNotNull(dto.getPrice(), "price");
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();

        applyDiscount(Boolean.TRUE.equals(dto.getIsDiscount()), dto.getDiscountRate());
        this.isRecommend = Boolean.TRUE.equals(dto.getIsRecommend());
    }

    private void applyAdminDto(AdminProductRequestDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("dto must not be null");
        }

        this.productName = requireText(dto.getProductName(), "productName");
        this.productType = requireNotNull(dto.getProductType(), "productType");
        this.price = requireNotNull(dto.getPrice(), "price");
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();

        applyDiscount(Boolean.TRUE.equals(dto.getIsDiscount()), dto.getDiscountRate());
        this.isRecommend = Boolean.TRUE.equals(dto.getIsRecommend());
    }

    private void applyDiscount(boolean discount, Integer rate) {
        this.isDiscount = discount;

        if (!discount) {
            this.discountRate = null;
            return;
        }

        this.discountRate = (rate == null ? 0 : rate);
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

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static <T> T requireNotNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}