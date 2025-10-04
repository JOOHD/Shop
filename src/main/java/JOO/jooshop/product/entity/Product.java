package JOO.jooshop.product.entity;

import JOO.jooshop.admin.products.model.AdminProductEntityMapperDto;
import JOO.jooshop.contentImgs.entity.ContentImages;
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

@Entity
@Getter
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
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductThumbnail> productThumbnails = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentImages> contentImages = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<ProductManagement> productManagements = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<WishList> wishLists = new ArrayList<>();

    private Long wishListCount;

    public Product() {}

    /** AdminProductRequestDto 기반 생성자 */
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

    /** Admin용 DTO 업데이트 (기존 유지) */
    public void updateFromDto(AdminProductEntityMapperDto dto) {
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

    /** 기존 ProductRequestDto 기반 생성자 */
    public Product(ProductRequestDto dto) {
        updateFromRequestDto(dto);
        this.createdAt = LocalDateTime.now();
    }

    /** 일반 API용 DTO 업데이트 */
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

