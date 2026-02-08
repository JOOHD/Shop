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
import java.util.Collections;
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

    /** ✅ 썸네일 (생명주기 완전 일치) */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ProductThumbnail> productThumbnails = new ArrayList<>();

    /** ✅ 상세 이미지 (생명주기 완전 일치) */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ContentImages> contentImages = new ArrayList<>();

    /** ✅ 옵션/재고 (생명주기 완전 일치) */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ProductManagement> productManagements = new ArrayList<>();

    /** ✅ 위시리스트는 사용자 활동 데이터라 cascade/orphanRemoval 없이 조회용만 */
    @OneToMany(mappedBy = "product")
    private final List<WishList> wishLists = new ArrayList<>();

    /** 파생값이면 제거 권장(유지한다면 서비스 레벨에서만 변경) */
    private Long wishListCount;

    /** 더미 플래그 */
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
        p.productName = requireText(productName, "productName");
        p.productType = type;
        p.price = requireNotNull(price, "price");
        p.productInfo = productInfo;
        p.manufacturer = manufacturer;

        p.applyDiscount(Boolean.TRUE.equals(isDiscount), discountRate);
        p.isRecommend = Boolean.TRUE.equals(isRecommend);

        p.dummy = true;
        return p;
    }

    /* =========================
       Read-only views (외부 노출용)
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
       Convenience methods (양방향 정합성 보장)
    ========================= */

    public void addThumbnailPath(String imagePath) {
        String path = requireText(imagePath, "imagePath");
        ProductThumbnail thumbnail = ProductThumbnail.create(this, path);
        this.productThumbnails.add(thumbnail);
    }

    public void removeThumbnail(ProductThumbnail thumbnail) {
        if (thumbnail == null) return;
        this.productThumbnails.remove(thumbnail);
    }

    public void addContentImagePath(String imagePath, UploadType uploadType) {
        String path = requireText(imagePath, "imagePath");
        if (uploadType == null) throw new IllegalArgumentException("uploadType must not be null");

        ContentImages image = ContentImages.create(this, path, uploadType);
        this.contentImages.add(image);
    }

    public void removeContentImage(ContentImages image) {
        if (image == null) return;
        this.contentImages.remove(image);
    }

    /**
     * ✅ 옵션(재고) 추가
     * - 리스트에만 넣지 말고 "pm.product"가 반드시 this를 가리키게 보장해야 FK가 안정적이다.
     * - pm을 외부에서 만들어 올 수도 있으니 안전 장치로 attach 수행.
     */
    public void addProductManagement(ProductManagement pm) {
        if (pm == null) return;
        pm.attachTo(this); // ✅ 아래 ProductManagement에 attachTo() 추가 필요
        this.productManagements.add(pm);
    }

    public void removeProductManagement(ProductManagement pm) {
        if (pm == null) return;
        this.productManagements.remove(pm); // orphanRemoval=true → flush 시 DELETE
        pm.detach(); // 안전(선택)
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
        if (dto == null) throw new IllegalArgumentException("dto must not be null");

        this.productName = requireText(dto.getProductName(), "productName");
        this.productType = dto.getProductType();
        this.price = requireNotNull(dto.getPrice(), "price");
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();

        applyDiscount(Boolean.TRUE.equals(dto.getIsDiscount()), dto.getDiscountRate());
        this.isRecommend = Boolean.TRUE.equals(dto.getIsRecommend());
    }

    private void applyAdminDto(AdminProductRequestDto dto) {
        if (dto == null) throw new IllegalArgumentException("dto must not be null");

        this.productName = requireText(dto.getProductName(), "productName");
        this.productType = dto.getProductType();
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
        // 할인인데 rate가 null이면 정책상 0으로 둘지, 예외로 막을지 선택.
        // 운영에서 안정적으로 가려면 "null이면 0" 추천.
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
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    private static <T> T requireNotNull(T value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}
