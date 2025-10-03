package JOO.jooshop.admin.products.controller;

import JOO.jooshop.admin.products.service.AdminProductService;
import JOO.jooshop.product.model.ProductApiDto;
import JOO.jooshop.product.model.ProductRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductApiController {

    private final AdminProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductApiDto>> list() {
        return ResponseEntity.ok(productService.findAllProduct());
    }

    @PostMapping
    public ResponseEntity<ProductApiDto> createProduct(@RequestBody ProductRequestDto dto) {
        ProductApiDto saved = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductApiDto> updateProduct(@PathVariable Long id,
                                                @RequestBody ProductRequestDto dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
