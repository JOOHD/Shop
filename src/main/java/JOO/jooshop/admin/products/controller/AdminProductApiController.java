package JOO.jooshop.admin.products.controller;

import JOO.jooshop.admin.products.model.AdminProductRequestDto;
import JOO.jooshop.admin.products.model.AdminProductResponseDto;
import JOO.jooshop.admin.products.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductApiController {

    private final AdminProductService productService;

    /** 상품 전체 조회 */
    @GetMapping
    public ResponseEntity<List<AdminProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.findAllProduct());
    }

    /** 상품 등록 (URL 기반 이미지) */
    @PostMapping
    public ResponseEntity<AdminProductResponseDto> createProduct(
            @RequestBody AdminProductRequestDto dto
    ) {
        return ResponseEntity.ok(productService.createProduct(dto));
    }

    /** 상품 수정 (URL 기반 이미지) */
    @PutMapping("/{id}")
    public ResponseEntity<AdminProductResponseDto> updateProduct(
            @PathVariable Long id,
            @RequestBody AdminProductRequestDto dto
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    /** 상품 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
