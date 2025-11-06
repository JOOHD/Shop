package JOO.jooshop.admin.products.model;

import JOO.jooshop.product.entity.enums.Gender;
import JOO.jooshop.product.entity.enums.ProductType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AdminProductRequestDto {

    // 기본 상품 정보
    private String productName;
    private ProductType productType;
    private BigDecimal price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount = false;
    private Integer discountRate = 0;
    private Boolean isRecommend = false;

    // 이미지 URL
    private String thumbnailUrl;        // 썸네일
    private List<String> contentUrls;  // 상세 이미지 리스트

    // 옵션 리스트 (ProductManagement와 연결)
    private List<ProductManagementDto> options;

    // 옵션 DTO (프론트에서 입력)
    @Getter
    @Setter
    public static class ProductManagementDto {
        private String color;
        private String category;
        private String size;
        private Gender gender;
        private Long stock;
    }

    // Entity → DTO 변환 (조회 용, 클라이언트에게 보여주기 위한 Entity 를 바꾸는 것)
    public static AdminProductRequestDto from(JOO.jooshop.product.entity.Product product) {
        AdminProductRequestDto dto = new AdminProductRequestDto();
        dto.setProductName(product.getProductName());
        dto.setProductType(product.getProductType());
        dto.setPrice(product.getPrice());
        dto.setProductInfo(product.getProductInfo());
        dto.setManufacturer(product.getManufacturer());
        dto.setIsDiscount(product.getIsDiscount());
        dto.setDiscountRate(product.getDiscountRate());
        dto.setIsRecommend(product.getIsRecommend());
        return dto;
    }
}
