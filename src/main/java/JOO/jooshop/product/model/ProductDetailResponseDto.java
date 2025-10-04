package JOO.jooshop.product.model;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import JOO.jooshop.wishList.model.WishListDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(of = "productId")
@AllArgsConstructor
public class ProductDetailResponseDto {
    /**
     * 목적: 회원용 상품 상세 조회 API에서 반환되는 DTO
     *
     * - 옵션, 썸네일, 찜한 사용자 정보까지 포함
     * - HTML 렌더링보다는 JSON API 응답용
     */

    private Long productId;                 // 상품 PK
    ProductType productType;                // 성별
    private String productName;             // 상품명
    private BigDecimal price;               // 상품 가격
    private String productInfo;             // 상품 정보
    private LocalDateTime createdAt;        // 생성일
    private LocalDateTime updatedAt;        // 수정일
    private String manufacturer;            // 제조자
    private boolean isDiscount;             // 할인여부 (true: 할인 중, false: 할인 아님)
    private Integer discountRate;           // 할인율
    private boolean isRecommend;            // 추천 여부 (true: 추천 상품, false: 일반 상품)
    private List<WishListDto> wishLists;    // 찜한 사용자 목록 (DTO LIST)
    private Long wishListCount;             // 찜한 수

    // 상세용 필드 추가
    private List<ProductManagement> options;   // 상품 옵션
    private List<String> productThumbnails;   // 썸네일 경로
    private Long inventoryId;                 // 기본 옵션 inventoryId
    private String thumbnailUrl;              // 대표 이미지

    public ProductDetailResponseDto(Product product) {
        this.productId = product.getProductId();
        this.productName = product.getProductName();
        this.price = product.getPrice();
        this.productInfo = product.getProductInfo();
        this.manufacturer = product.getManufacturer();
        this.isDiscount = product.getIsDiscount();
        this.discountRate = product.getDiscountRate();
        this.isRecommend = product.getIsRecommend();
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
        this.wishLists = product.getWishLists() != null
                ? product.getWishLists().stream().map(WishListDto::new).collect(Collectors.toList())
                : Collections.emptyList();
        this.wishListCount = product.getWishListCount();
        this.options = product.getProductManagements();
        this.productThumbnails = product.getProductThumbnails().stream()
                .map(ProductThumbnail::getImagePath)
                .collect(Collectors.toList());
        this.thumbnailUrl = this.productThumbnails.isEmpty() ? "" : this.productThumbnails.get(0);
        this.inventoryId = !options.isEmpty() ? options.get(0).getInventoryId() : null;
    }

    /**
     * 🔹 builder 스타일 체이닝 메서드 추가
     */
    public ProductDetailResponseDto withInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
        return this;
    }

}

