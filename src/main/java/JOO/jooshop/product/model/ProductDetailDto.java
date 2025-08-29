package JOO.jooshop.product.model;

import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.model.ProductManagementDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 상세 정보 DTO
 *
 * - 상품 단일 조회용
 * - HTML 렌더링 시 필요한 필드만 포함
 * - ProductManagement 옵션 리스트를 DTO로 변환하여 Thymeleaf에서 반복 처리 가능
 */
@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailDto {

    private Long productId;
    private Long inventoryId;         // 대표 옵션(첫 번째 ProductManagement) PK
    private ProductType productType;
    private String productName;
    private BigDecimal price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount;
    private Integer discountRate;
    private Boolean isRecommend;

    private String thumbnailUrl;      // 대표 이미지 URL
    private List<String> contentImages; // 컨텐츠 이미지 리스트

    // ProductManagement 옵션 리스트 (사이즈 등)
    private List<ProductManagementDto> sizes;

    /**
     * 대표 옵션 inventoryId 세팅 메서드
     */
    public ProductDetailDto withInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
        return this;
    }

    /**
     * 상품 Entity + ProductManagement 리스트 + 대표 이미지 URL → DTO 변환
     *
     * @param product          Product Entity
     * @param productManagements ProductManagement 옵션 리스트
     * @param thumbnailUrl     대표 이미지 URL
     */
    public ProductDetailDto(Product product, List<ProductManagement> productManagements, String thumbnailUrl) {
        this.productId = product.getProductId();
        this.productType = product.getProductType();
        this.productName = product.getProductName();
        this.price = product.getPrice();
        this.productInfo = product.getProductInfo();
        this.manufacturer = product.getManufacturer();
        this.isDiscount = product.getIsDiscount();
        this.discountRate = product.getDiscountRate();
        this.isRecommend = product.getIsRecommend();
        this.thumbnailUrl = thumbnailUrl;

        // ContentImages → 문자열 리스트 변환
        this.contentImages = product.getContentImages().stream()
                .map(ContentImages::getImagePath)
                .collect(Collectors.toList());

        // ProductManagement → ProductManagementDto 변환
        this.sizes = productManagements.stream()
                .map(ProductManagementDto::from) // 기존 DTO의 from 메서드 사용
                .collect(Collectors.toList());

        // 대표 옵션 inventoryId 세팅 (첫 번째 옵션 기준)
        if (!productManagements.isEmpty()) {
            this.inventoryId = productManagements.get(0).getInventoryId();
        }
    }

    /**
     * 화면에서 대표 옵션 사이즈 이름 출력용
     */
    public String getSizeDescription() {
        return (sizes != null && !sizes.isEmpty())
                ? sizes.get(0).getSize().getDescription()
                : "";
    }
}
