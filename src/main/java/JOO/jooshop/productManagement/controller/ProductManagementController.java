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

import java.util.List;
import java.util.stream.Collectors;

import static JOO.jooshop.global.exception.ResponseMessageConstants.DELETE_SUCCESS;

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
                .map(ProductManagementDto::from)
                .collect(Collectors.toList());
        return new ResponseEntity<>(collect, HttpStatus.OK);
    }

    /**
     * 상품 관리 id로 조회
     * @param inventoryId
     * @return
     */
    @GetMapping("/{inventoryId}")
    public ResponseEntity<ProductManagementDto> getInventoryById(@PathVariable("inventoryId") Long inventoryId) {
        ProductManagement inventoryDetail = managementService.inventoryDetail(inventoryId);
        ProductManagementDto productManagementDto = ProductManagementDto.from(inventoryDetail);
        return new ResponseEntity<>(productManagementDto, HttpStatus.OK);
    }

    /**
     * 상품 관리 등록
     * @param requestDto
     * @return
     */
    @PostMapping("/new")
    public ResponseEntity<ProductManagementDto> createInventory(@Valid @RequestBody InventoryCreateDto requestDto) {
        ProductManagement saved = managementService.createInventory(requestDto);
        return ResponseEntity.ok(ProductManagementDto.from(saved));
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


// 아직도 조회 테스트가 에러 발생 (db 에 값이 안 넣어짐,, get 조회는 여러 방식으로 문제가 있네)