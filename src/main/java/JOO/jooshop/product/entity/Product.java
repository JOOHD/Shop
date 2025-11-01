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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
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

    /** dummy data factory method */
    public static Product createProductById(Long productId) {
        return Product.builder()
                .productId(productId)
                .productName("Product_" + productId)
                .price(BigDecimal.valueOf(10000))
                .build();
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

        // 기존 옵션 초기화
        this.getProductManagements().clear();

        // 새로운 옵션 추가

        for (AdminProductRequestDto.ProductManagementDto dto : optionDto) {

            // Color 매핑 (예: 이름 기반 매핑)
            ProductColor color = ProductColor.ofName(dto.getColor());
            // 실제 구현에서는 ProductColorRepository.findByName(dto.getColor()) 등으로 조회 가능

            // Category 매핑 (예: 이름 기반 매핑)
            Category category = Category.ofName(dto.getCategory());
            // 실제 구현에서는 CategoryRepository.findByName(dto.getCategory()) 등으로 조회

            // Size enum 변환
            Size size = Size.valueOf(dto.getSize());

            // ProductManagement 엔티티 생성
            ProductManagement pm = ProductManagement.builder()
                    .product(this)       // 현재 Product 엔티티
                    .color(color)
                    .category(category)
                    .gender(dto.getGender())
                    .size(size)
                    .initialStock(dto.getStock())
                    .productStock(dto.getStock())
                    .build();

            this.getProductManagements().add(pm);
        }
    }
}

