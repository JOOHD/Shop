package JOO.jooshop.productManagement.service;

import JOO.jooshop.categorys.repository.CategoryRepository;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
import JOO.jooshop.product.repository.ProductColorRepositoryV1;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.model.InventoryCreateDto;
import JOO.jooshop.productManagement.repository.ProductManagementRepository;
import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.global.authorization.RequiresRole;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.model.InventoryUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static JOO.jooshop.global.exception.ResponseMessageConstants.PRODUCT_NOT_FOUND;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductManagementService {
    public final ProductManagementRepository productManagementRepository;
    public final CategoryRepository categoryRepository;
    public final ProductRepositoryV1 productRepository;
    public final ProductColorRepositoryV1 productColorRepository;

    /**
     * 상품관리 등록
     * @param requestDto
     * @return
     */
    @Transactional
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public ProductManagement createInventory(InventoryCreateDto requestDto) {
        // 연관 엔팉티 실제 DB에서 조회
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다."));
        ProductColor color = productColorRepository.findById(requestDto.getColorId())
                .orElseThrow(() -> new IllegalArgumentException("해당 색상이 존재하지 않습니다."));
        Category category = categoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리가 존재하지 않습니다."));

        // DTO -> Entity 변환 (진짜 객체 주입)
        ProductManagement entity = requestDto.toEntity(product, color, category);

        // 중복 체크
        ProductManagement existingInventory = productManagementRepository
                .findByProductAndColorAndCategoryAndSize(product, color, category, entity.getSize())
                .orElse(null);

        if (existingInventory != null) {
            throw new IllegalArgumentException("이미 존재하는 상품입니다.");
        }

        // 저장
        return productManagementRepository.save(entity);
    }

    /**
     * 상품 관리 정보 조회 - 상품 하나 id로 찾기
     * @param inventoryId
     * @return
     */
    public ProductManagement inventoryDetail(Long inventoryId) {
        return productManagementRepository.findById(inventoryId).get();
    }

    /**
     * 상품 관리 정보 조회 - 모든 상품 찾기
     * @return
     */
    public List<ProductManagement> allInventory() {
        return productManagementRepository.findAll();
    }

    /**
     * 상품 관리 수정
     * @param inventoryId
     * @param request
     * @return
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public ProductManagement updateInventory(Long inventoryId, InventoryUpdateDto request) {

        ProductManagement existingInventory = productManagementRepository.findById(inventoryId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        Long productStock = existingInventory.getProductStock() + request.getAdditionalStock();
        Category category = categoryRepository.findByCategoryId(request.getCategoryId()).orElseThrow(() -> new NoSuchElementException("카테고리를 찾을 수 없습니다."));

        // 카테고리 변경
        existingInventory.changeCategory(category);

        // 추가 입고
        Long add = request.getAdditionalStock();
        if (add != null && add > 0) {
            existingInventory.restock(add);
        }

        // restock 가능 여부
        if (request.getIsRestockAvailable() != null) {
            existingInventory.setRestockAvailable(request.getIsRestockAvailable());
        }


//        InventoryUpdateDto.updateInventoryForm(existingInventory, request);

        return productManagementRepository.save(existingInventory);
    }

    /**
     * 상품 관리 삭제
     * @param inventoryId
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void deleteInventory(Long inventoryId) {
        ProductManagement existingInventory = productManagementRepository.findById(inventoryId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));
        productManagementRepository.delete(existingInventory);
    }
}
