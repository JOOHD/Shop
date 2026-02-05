package JOO.jooshop.admin.products.model;

import JOO.jooshop.product.entity.enums.Gender;
import JOO.jooshop.product.entity.enums.ProductType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AdminProductRequestDto {

    /**
     * ✅ 요청 DTO 정책
     * - options == null : "옵션 변경 없음"
     * - options.isEmpty() : "옵션 전체 삭제"
     * - options has values : "옵션 Replace(전체 삭제 후 재삽입)"
     *
     * 이미지 정책(현재 컨트롤러 기준)
     * - 썸네일/상세 이미지는 MultipartFile로 받는다.
     * - 따라서 thumbnailUrl/contentUrls는 "외부 URL 입력 방식"을 쓰지 않는다면 제거하는 게 최선.
     *   (혼용하면 정책이 깨지기 쉬움)
     */

    /* =========================
       Product fields
    ========================= */

    private String productName;
    private ProductType productType;
    private BigDecimal price;
    private String productInfo;
    private String manufacturer;

    private Boolean isDiscount = false;
    private Integer discountRate = 0;
    private Boolean isRecommend = false;

    /* =========================
       (Optional) External URL fields
       - MultipartFile 업로드만 쓰면 제거 권장
    ========================= */

    // 외부 URL 방식을 같이 지원하고 싶을 때만 사용 (그 외엔 null 유지 권장)
    private String thumbnailUrl;
    private List<String> contentUrls;

    /* =========================
       Options
    ========================= */

    private List<ProductManagementDto> options;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductManagementDto {
        private String color;
        private String category;
        private String size;
        private Gender gender;
        private Long stock;

        /** 서비스에서 쓰기 좋게 최소 정규화 */
        public void normalize() {
            if (color != null) color = color.trim();
            if (category != null) category = category.trim();
            if (size != null) size = size.trim();
        }
    }

    /* =========================
       Validation / Normalization
       - 컨트롤러/서비스 진입 시 1회 호출 권장
    ========================= */

    public void normalizeAndValidate() {
        if (productName != null) productName = productName.trim();
        if (productInfo != null) productInfo = productInfo.trim();
        if (manufacturer != null) manufacturer = manufacturer.trim();
        if (thumbnailUrl != null) thumbnailUrl = thumbnailUrl.trim();

        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName is required");
        }
        if (price == null) {
            throw new IllegalArgumentException("price is required");
        }
        if (productType == null) {
            throw new IllegalArgumentException("productType is required");
        }

        boolean discount = Boolean.TRUE.equals(isDiscount);
        if (!discount) {
            // 할인 아니면 할인율은 의미 없으니 0/null로 정리 (정책 택1)
            discountRate = 0;
        } else {
            if (discountRate == null) discountRate = 0;
            if (discountRate < 0 || discountRate > 100) {
                throw new IllegalArgumentException("discountRate must be between 0 and 100");
            }
        }

        if (options != null) {
            for (ProductManagementDto opt : options) {
                if (opt == null) continue;
                opt.normalize();

                // 옵션 값이 들어온 케이스에서 최소 검증
                if (opt.gender == null) {
                    throw new IllegalArgumentException("option.gender is required");
                }
                if (opt.size == null || opt.size.isBlank()) {
                    throw new IllegalArgumentException("option.size is required");
                }
                if (opt.color == null || opt.color.isBlank()) {
                    throw new IllegalArgumentException("option.color is required");
                }
                if (opt.category == null || opt.category.isBlank()) {
                    throw new IllegalArgumentException("option.category is required");
                }
                if (opt.stock != null && opt.stock < 0) {
                    throw new IllegalArgumentException("option.stock must be >= 0");
                }
            }
        }
    }

    /** 서비스 로직에서 자주 쓰는 헬퍼 */
    public boolean hasOptionsField() {
        return options != null; // null이면 "옵션 변경 없음"
    }

    public boolean isOptionsClearRequest() {
        return options != null && options.isEmpty(); // 빈 리스트면 "전체 삭제"
    }
}
