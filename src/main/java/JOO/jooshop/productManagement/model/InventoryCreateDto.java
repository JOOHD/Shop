package JOO.jooshop.productManagement.model;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.entity.enums.Size;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryCreateDto {

    /*
        dto -> entity : toEntity 변환 메서드 사용 (저장 요청)
        - 컨트롤러에서 dto 받음 -> toEntity() 호출, entity 생성 -> 저장
        
        entity -> dto :  InventoryCreateDto 변환 생성자 사용 (조회 요청)
        - 서비스에서 entity 가져옴 -> new InventoryCreateDto(entity) 호출 -> DTO 변환 응답
     */
    @NotNull(message = "상품은 필수로 지정해야 합니다.")
    private Long productId;
    @NotNull(message = "색상은 필수로 지정해야 합니다.")
    private Long colorId;
    @NotNull(message = "카테고리는 필수로 지정해야 합니다.")
    private Long categoryId;
    @NotNull(message = "사이즈는 필수로 지정해야 합니다.")
    private Size size;

    private Long initialStock;
    private Long additionalStock = 0L; // 생성할 땐 무조건 0
//    private Long productStock; // 생성할 땐 productStock = initialStock
    private Boolean isRestockAvailable = false;
    private Boolean isRestocked = false;
    private Boolean isSoldOut = false;

    public InventoryCreateDto(ProductManagement productManagement) {
        this(
                productManagement.getProduct().getProductId(),
                productManagement.getColor().getColorId(),
                productManagement.getCategory().getCategoryId(),
                productManagement.getSize(),
                productManagement.getInitialStock(),
                productManagement.getAdditionalStock(),
                productManagement.isRestockAvailable(),
                productManagement.isRestocked(),
                productManagement.isSoldOut()
        );
    }

    /*
        - DB에서 실제로 조회하지 않고, 단순히 ID만 가진 "가짜 프록시 객체"를 만들어서 Entity 에 할당
        - 밑에 구조는 DB에 있는 실제 객체를 로딩하지 않기 때문에, product.getProductName() 같은 값을
        접근하려 할 때, Hibernate 가 해당 객체를 초기화할 수 없음 -> 결과적으로 null 로 나오는 현상 발생

        Product product = Product.createProductById(this.productId);
        ProductColor color = ProductColor.createProductColorById(this.colorId);
        Category category = Category.createCategoryById(this.categoryId);

        해결 방법
        - 위에 코드를 서비스 클래스에 적용
    */
    public ProductManagement toEntity(Product product, ProductColor color, Category category) {
        return ProductManagement.of(
                product,
                color,
                category,
                this.size,
                this.initialStock,
                this.isRestockAvailable,
                this.isRestocked,
                this.isSoldOut
        );
    }

}
