package JOO.jooshop.product.model;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
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
public class ProductDto {

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


    public ProductDto(Product product) {
        this(
                product.getProductId(),
                product.getProductType(),
                product.getProductName(),
                product.getPrice(),
                product.getProductInfo(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getManufacturer(),
                product.getIsDiscount(),
                product.getDiscountRate(),
                product.getIsRecommend(),
                product.getWishLists() != null ? product.getWishLists().stream().map(WishListDto::new).collect(Collectors.toList()) : Collections.emptyList(),
                product.getWishListCount()
        );
    }

}

