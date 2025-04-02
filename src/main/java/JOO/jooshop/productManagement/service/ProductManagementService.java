package JOO.jooshop.productManagement.service;

import JOO.jooshop.categorys.entity.Category;
import JOO.jooshop.categorys.repository.CategoryRepository;
import JOO.jooshop.global.authorization.RequiresRole;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.model.InventoryUpdateDto;
import JOO.jooshop.productManagement.repository.ProductManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static JOO.jooshop.global.ResponseMessageConstants.PRODUCT_NOT_FOUND;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductManagementService {

    public final ProductManagementRepository productManagementRepository;
    public final CategoryRepository categoryRepository;

    /**
     * 상품관리 등록
     * @param request
     * @return
     */
    @Transactional
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public Long createInventory(ProductManagement request) {

        // 1. 기존 상품 조회 (상품, 색상, 카테고리, 사이즈 조합으로 중복 확인)
        ProductManagement existingInventory = productManagementRepository
                .findByProductAndColorAndCategoryAndSize(request.getProduct(), request.getColor(), request.getCategory(), request.getSize())
                .orElse(null);

        // 2. 이미 존재하는 경우 예외 처리
        if (existingInventory != null) {
            throw new IllegalArgumentException("이미 존재하는 상품입니다.");
        }

        // 3. 새로운 상품 등록
        productManagementRepository.save(request);

        // 4. 저장된 상품의 inventoryId 반환
        return request.getInventoryId();
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
     * 
     * 1. InventoryUpdateDto 에 클라이언트 요청 데이터를 바인딩함
     * 2. 컨트롤러나 서비스에서 해당 DTO 값을 엔티티에 적용
     * 3. ProductManagement.udpateInventory 메서드 호출해서 상태 업데이트
     * 4. 추가로 이 엔티티를 save() 하면 DB 에 반영됨
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    @Transactional
    public ProductManagement updateInventory(Long inventoryId, InventoryUpdateDto request) {

        // 1. 기존 엔티티 조회
        ProductManagement existingInventory = productManagementRepository.findById(inventoryId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        // 2. 카테고리 조회
        Category category = categoryRepository.findByCategoryId(request.getCategoryId())
                .orElseThrow(() -> new NoSuchElementException("카테고리를 찾을 수 없습니다."));

        // 3. 재고 업데이트 로직 (기존 재고 + 추가 재고)
        Long productStock = existingInventory.getProductStock() + request.getAdditionalStock();

        // 4. DTO -> Entity 매핑 및 업데이트
        existingInventory.updateInventory(
                category,
                productStock,
                request.getAdditionalStock(),
                request.getIsRestockAvailable(),
                request.getIsRestocked(),
                request.getIsSoldOut()
        );

        // 5. 변경된 엔티티 저장
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
