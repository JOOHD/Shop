package JOO.jooshop.product.model;

import JOO.jooshop.global.validation.ValidDiscountRate;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.enums.ProductType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidDiscountRate // 커스텀 유효성 검사 애노테이션 적용
public class ProductCreateDto {
    @NotBlank(message = "상품 이름은 필수입니다.")
    private String productName;
    private ProductType productType;
    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Integer price;
    private String productInfo;
    private String manufacturer;
    private Boolean isDiscount = false;
    @Max(value = 100, message = "할인율은 100을 초과할 수 없습니다.")
    private Integer discountRate = null;
    private Boolean isRecommend = false;


    /*
        Q.Response 가 아닌, Request 클래스에서 왜? 변환 메서드가 필요하지?

        1. 클라이언트가 상품 등록 요청을 보냄
            → ProductCreateDto에 상품 데이터가 담김

        2. 서버에서 유효성 검사를 진행
            - 만약 실패하면 → 에러 메시지와 함께 입력했던 데이터가 그대로 다시 클라이언트에 반환되어야 합니다.
            - 그래야 사용자가 다시 입력하지 않고 수정만 해서 재시도할 수 있습니다.

        Q.만약 이 변환 메서드가 없으면?
	        - 클라이언트가 작성했던 모든 값이 사라지고, 다시 처음부터 입력해야 하는 불편함이 생깁니다.

	    Q.그럼 생성자를 쓰면 값 변경이 아예 안 되잖아?"
            - 그렇기 때문에 변경이 필요한 경우에는 새로운 객체를 만들어 반환하는 방식으로 처리
     */
    public ProductCreateDto(Product product) {
        this(
                product.getProductName(),
                product.getProductType(),
                product.getPrice(),
                product.getProductInfo(),
                product.getManufacturer(),
                product.getIsDiscount(),
                product.getDiscountRate(),
                product.getIsRecommend()
        );
    }

    // isDiscount가 false 이라면 할인율 null
    public void setIsDiscount(Boolean isDiscount) {
        this.isDiscount = isDiscount;
        if (Boolean.FALSE.equals(isDiscount)) {
            this.discountRate = null;
        }
    }

}
