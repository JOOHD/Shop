package JOO.jooshop.productManagement.service;

import JOO.jooshop.categorys.repository.CategoryRepository;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.entity.ProductColor;
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
    public Long createInventory(InventoryCreateDto request) {
        ProductManagement entity = InventoryCreateDto.newRequestManagementForm(request);

        // 중복 체크, 저장
        ProductManagement existingInventory = productManagementRepository
                .findByProductAndColorAndCategoryAndSize(
                        entity.getProduct(), entity.getColor(), entity.getCategory(), entity.getSize()
                ).orElse(null);

        if (existingInventory != null) {
            throw new IllegalArgumentException("이미 존재하는 상품입니다.");
        }

        productManagementRepository.save(entity);
        return entity.getInventoryId();
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

        existingInventory.updateInventory(category, request.getAdditionalStock(), productStock, request.getIsRestockAvailable(), request.getIsRestocked(),request.getIsSoldOut());

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
