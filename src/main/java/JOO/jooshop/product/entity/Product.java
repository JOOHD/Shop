package JOO.jooshop.product.entity;

import JOO.jooshop.admin.products.model.AdminProductRequestDto;
import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.product.model.ProductRequestDto;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import JOO.jooshop.wishList.entity.WishList;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor  // JPA용 기본 생성자
@Table(name = "products_table")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @NotBlank
    private String productName;

    @NotNull
    private BigDecimal price;

    private String productInfo;

    private String manufacturer;

    private Boolean isDiscount = false;

    private Integer discountRate;

    private Boolean isRecommend = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductThumbnail> productThumbnails = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentImages> contentImages = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<ProductManagement> productManagements = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<WishList> wishLists = new ArrayList<>();

    private Long wishListCount;

    /* persist() 전에 자동으로 createdAt, updatedAt 세팅 */
    @PrePersist
    public void onCreate() {
        this.createdAt = (this.createdAt == null) ? LocalDateTime.now() : this.createdAt;
        this.updatedAt = (this.updatedAt == null) ? LocalDateTime.now() : this.updatedAt;
    }

    /** 옵션(ProductManagement) 등록/업데이트 */
    public void updateProductManagements(List<AdminProductRequestDto.ProductManagementDto> optionDto) {
        this.productManagements.clear(); // 기존 옵션 제거
        for (AdminProductRequestDto.ProductManagementDto dto : optionDto) {
            ProductManagement pm = new ProductManagement();
            pm.setProduct(this);
            pm.setSize(Size.valueOf(dto.getSize()));
            pm.setInitialStock(dto.getStock());
            // 색상, 카테고리 연결 가능
            this.productManagements.add(pm);
        }
    }

    /* update 시 자동으로 updatedAt 변경 */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /* dummy data factory method */
    public static Product createProductById(Long productId) {
        return Product.builder()
                .productId(productId)
                .productName("Product_" + productId)
                .price(BigDecimal.valueOf(10000)) // default
                .build();
    }

    /* AdminProductRequestDto 기반 생성자 */
    public Product(JOO.jooshop.admin.products.model.AdminProductRequestDto dto) {
        this.productName = dto.getProductName();
        this.productType = dto.getProductType();
        this.price = dto.getPrice();
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();
        this.isDiscount = dto.getIsDiscount();
        this.discountRate = Boolean.TRUE.equals(dto.getIsDiscount()) ? dto.getDiscountRate() : null;
        this.isRecommend = dto.getIsRecommend();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /* Admin용 DTO 업데이트 */
    public void updateFromDto(AdminProductRequestDto dto) {
        this.productName = dto.getProductName();
        this.productType = dto.getProductType();
        this.price = dto.getPrice();
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();
        this.isDiscount = dto.getIsDiscount();
        this.discountRate = Boolean.TRUE.equals(dto.getIsDiscount()) ? dto.getDiscountRate() : null;
        this.isRecommend = dto.getIsRecommend();
        this.updatedAt = LocalDateTime.now();
    }

    /* 기존 ProductRequestDto 기반 생성자 */
    public Product(ProductRequestDto dto) {
        updateFromRequestDto(dto);
        this.createdAt = LocalDateTime.now();
    }

    /* 일반 API용 DTO 업데이트 */
    public void updateFromRequestDto(ProductRequestDto dto) {
        this.productName = dto.getProductName();
        this.productType = dto.getProductType();
        this.price = dto.getPrice();
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();
        this.isDiscount = dto.getIsDiscount();
        this.discountRate = Boolean.TRUE.equals(dto.getIsDiscount()) ? dto.getDiscountRate() : null;
        this.isRecommend = dto.getIsRecommend();
        this.updatedAt = LocalDateTime.now();
    }
}
