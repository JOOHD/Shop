package JOO.jooshop.productManagement.controller;

import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.model.InventoryCreateDto;
import JOO.jooshop.productManagement.model.InventoryUpdateDto;
import JOO.jooshop.productManagement.model.ProductManagementDto;
import JOO.jooshop.productManagement.service.ProductManagementService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static JOO.jooshop.global.ResponseMessageConstants.DELETE_SUCCESS;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class ProductManagementController {

    private final ProductManagementService managementService;

    @Data
    private class UpdateResponse {
        private Long inventoryId;
        private Long productId;

        private UpdateResponse(Long inventoryId, Long productId) {
            this.inventoryId = inventoryId;
            this.productId = productId;
        }
    }

    /**
     * 전체 상품 관리 조회
     * @return
     */
    @GetMapping("")
    public ResponseEntity<List<ProductManagementDto>> inventoryList() {
        List<ProductManagement> inventoryList = managementService.allInventory();
        List<ProductManagementDto> collect = inventoryList.stream()
                .map(ProductManagementDto::new)
                .collect(Collectors.toList());
        return new ResponseEntity<>(collect, HttpStatus.OK);
    }

    /**
     * 상품 관리 id로 조회
     * @param inventoryId
     * @return
     */
    @GetMapping("/{inventoryId}")
    public ResponseEntity<ProductManagementDto> getInventoryById(@PathVariable Long inventoryId) {
        ProductManagement inventoryDetail = managementService.inventoryDetail(inventoryId);
        ProductManagementDto productManagementDto = new ProductManagementDto(inventoryDetail);
        return new ResponseEntity<>(productManagementDto, HttpStatus.OK);
    }

    /**
     * 상품 관리 등록
     * @param requestDto
     * @return
     */
    @PostMapping("/new")
    public ResponseEntity<ProductManagementDto> createInventory(@Valid @RequestBody InventoryCreateDto requestDto) {
        ProductManagement request = InventoryCreateDto.newRequestManagementForm(requestDto);
        Long createdId = managementService.createInventory(request);

        // Location 헤더 생성
        URI location = URI.create("/api/v1/inventory/" + createdId);

        return ResponseEntity.created(location) // 201 Created + Location header
                .body(new ProductManagementDto(request)); // ProductManagement 안되는 이유 = entity class
    }

    /**
     * 상품 관리 수정
     * @param inventoryId
     * @param request
     * @return
     */
    @PutMapping("/{inventoryId}")
    public ResponseEntity<String> updateInventory(@PathVariable("inventoryId") Long inventoryId, @Valid @RequestBody InventoryUpdateDto request) {
        ProductManagement updated = managementService.updateInventory(inventoryId, request);
        UpdateResponse response = new UpdateResponse(updated.getInventoryId(), updated.getProduct().getProductId());
        return ResponseEntity.ok().body("수정 완료" + response);
    }

    /**
     * 상품 관리 삭제
     * @param inventoryId
     * @return
     */
    @DeleteMapping("/{inventoryId}")
    public ResponseEntity<String> deleteInventory(@PathVariable("inventoryId") Long inventoryId) {
        managementService.deleteInventory(inventoryId);
        return ResponseEntity.ok().body(DELETE_SUCCESS);
    }

}
