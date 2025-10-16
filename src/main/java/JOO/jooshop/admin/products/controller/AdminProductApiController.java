package JOO.jooshop.admin.products.controller;

import JOO.jooshop.admin.products.model.AdminProductEntityMapperDto;
import JOO.jooshop.admin.products.model.AdminProductRequestDto;
import JOO.jooshop.admin.products.model.AdminProductResponseDto ;
import JOO.jooshop.admin.products.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductApiController {

    private final AdminProductService productService;

    /* 상품 조회 */
    @GetMapping
    public ResponseEntity<List<AdminProductResponseDto >> list() {
        return ResponseEntity.ok(productService.findAllProduct());
    }

    /* 상품 등록 */
    @PostMapping
    public ResponseEntity<AdminProductResponseDto > createProduct(
            @RequestPart("product") AdminProductRequestDto dto,
            @RequestPart(value = "images", required = false)List<MultipartFile> images) {
        AdminProductResponseDto saved = productService.createProduct(dto, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /* 상품 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<AdminProductResponseDto> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") AdminProductEntityMapperDto dto,
            @RequestPart(value = "images", required = false)List<MultipartFile> images) {
        return ResponseEntity.ok(productService.updateProduct(id, dto, images));
    }

    /* 상품 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
