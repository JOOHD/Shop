package JOO.jooshop.product.entity;

import JOO.jooshop.admin.products.model.AdminProductRequestDto;
import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.global.time.BaseEntity;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.product.model.ProductRequestDto;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import JOO.jooshop.wishList.entity.WishList;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "products_table")
public class Product extends BaseEntity {

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

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductThumbnail> productThumbnails = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentImages> contentImages = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<ProductManagement> productManagements = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<WishList> wishLists = new ArrayList<>();

    private Long wishListCount;

    /** DTO 기반 생성 */
    public static Product ofId(Long productId) {
        Product p = new Product();
        p.productId = productId;
        return p;
    }

    /** 정적 메서드 */
    public static Product createDummy(String productName, ProductType type, BigDecimal price,
                                      String productInfo, String manufacturer,
                                      Boolean isDiscount, Integer discountRate, Boolean isRecommend) {
        Product p = new Product();
        p.productName = productName;
        p.productType = type;
        p.price = price;
        p.productInfo = productInfo;
        p.manufacturer = manufacturer;
        p.isDiscount = isDiscount != null ? isDiscount : false;
        p.discountRate = isDiscount ? discountRate : null;
        p.isRecommend = isRecommend != null ? isRecommend : false;
        return p;
    }


    /** Admin DTO 기반 생성자 */
    public Product(AdminProductRequestDto dto) {
        this.productName = dto.getProductName();
        this.productType = dto.getProductType();
        this.price = dto.getPrice();
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();
        this.isDiscount = Boolean.TRUE.equals(dto.getIsDiscount()) ? dto.getIsDiscount() : false;
        this.discountRate = Boolean.TRUE.equals(dto.getIsDiscount()) ? dto.getDiscountRate() : null;
        this.isRecommend = dto.getIsRecommend();
        // createdAt/updatedAt는 BaseEntity에서 자동 관리
    }

    /** 일반 API DTO(ProductRequestDto) 생성자 */
    public Product(ProductRequestDto dto) {
        updateFromRequestDto(dto);
    }

    /** 일반 API DTO 업데이트 */
    public void updateFromRequestDto(ProductRequestDto dto) {
        this.productName = dto.getProductName();
        this.productType = dto.getProductType();
        this.price = dto.getPrice();
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();
        this.isDiscount = Boolean.TRUE.equals(dto.getIsDiscount()) ? dto.getIsDiscount() : false;
        this.discountRate = Boolean.TRUE.equals(dto.getIsDiscount()) ? dto.getDiscountRate() : null;
        this.isRecommend = dto.getIsRecommend();
    }

    /** Admin DTO 업데이트 */
    public void updateFromDto(AdminProductRequestDto dto) {
        this.productName = dto.getProductName();
        this.productType = dto.getProductType();
        this.price = dto.getPrice();
        this.productInfo = dto.getProductInfo();
        this.manufacturer = dto.getManufacturer();
        this.isDiscount = Boolean.TRUE.equals(dto.getIsDiscount()) ? dto.getIsDiscount() : false;
        this.discountRate = Boolean.TRUE.equals(dto.getIsDiscount()) ? dto.getDiscountRate() : null;
        this.isRecommend = dto.getIsRecommend();
    }

    /** 옵션(ProductManagement) 등록/업데이트 */
    public void updateProductManagements(List<AdminProductRequestDto.ProductManagementDto> optionDto) {
        if (optionDto == null || optionDto.isEmpty()) return;
        this.getProductManagements().clear();

        for (AdminProductRequestDto.ProductManagementDto dto : optionDto) {
            ProductColor color = ProductColor.ofName(dto.getColor());
            Category category = Category.ofName(dto.getCategory());
            Size size = Size.valueOf(dto.getSize());

            ProductManagement pm = ProductManagement.create(
                    this, color, category, dto.getGender(), size, dto.getStock()
            );

            this.getProductManagements().add(pm);
        }
    }
}

