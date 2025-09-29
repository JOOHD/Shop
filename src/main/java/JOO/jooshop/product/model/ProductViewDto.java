package JOO.jooshop.product.model;

import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.model.ProductManagementDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductViewDto {

    /**
     * 프론트 화면용 단일 상품 조회 DTO
     * - HTML/Thymeleaf 렌더링 전용
     * - 옵션/대표 이미지/컨텐츠 이미지 포함
     */

    private Long productId;
    private Long inventoryId; // 대표 옵션
    private ProductType productType;
    private String productName;
    private BigDecimal price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount;
    private Integer discountRate;
    private Boolean isRecommend;

    private String thumbnailUrl;           // 대표 이미지 URL
    private List<String> contentImages;    // 컨텐츠 이미지 리스트
    private List<ProductManagementDto> sizes; // 옵션 리스트 (사이즈 등)

    /**
     * 대표 옵션(inventoryId)을 세팅하는 메서드
     * - 기존 ProductManagement 리스트에서 선택된 대표 옵션을 외부에서 지정할 때 사용
     *
     * @param inventoryId 대표 옵션 ID
     * @return this 객체 반환 (체이닝 가능)
     */
    public ProductViewDto withInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
        return this;
    }

    /**
     * Product 엔티티 + ProductManagement 옵션 리스트 + 대표 이미지 URL → DTO 변환
     * - 화면 렌더링용으로 필요한 필드만 추출
     * - ProductManagementDto로 옵션 리스트 변환
     * - 대표 옵션(inventoryId)을 첫 번째 옵션으로 기본 설정
     *
     * @param product          상품 엔티티
     * @param productManagements 상품 옵션 리스트
     * @param thumbnailUrl     대표 이미지 URL
     */
    public ProductViewDto(Product product, List<ProductManagement> productManagements, String thumbnailUrl) {
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

        // ContentImages → 문자열 리스트로 변환
        this.contentImages = product.getContentImages().stream()
                .map(ContentImages::getImagePath) // map -> 변환 (추가x)
                .collect(Collectors.toList());

        // ProductManagement → ProductManagementDto 변환
        this.sizes = productManagements.stream()
                .map(ProductManagementDto::from)
                .collect(Collectors.toList());

        // 대표 옵션 inventoryId 세팅 (첫 번째 옵션 기준)
        if (!productManagements.isEmpty()) {
            this.inventoryId = productManagements.get(0).getInventoryId();
        }
    }

    /**
     * 화면에서 대표 옵션 사이즈 이름 출력용
     * - sizes 리스트가 비어있지 않으면 첫 번째 옵션의 사이즈 설명 반환
     *
     * @return 대표 옵션 사이즈 설명 (없으면 빈 문자열)
     */
    public String getSizeDescription() {
        return (sizes != null && !sizes.isEmpty())
                ? sizes.get(0).getSize().getDescription()
                : "";
    }
}
