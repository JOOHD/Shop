package JOO.jooshop.admin.products.service;

import JOO.jooshop.admin.products.dto.ProductDto;
import JOO.jooshop.admin.products.repository.AdminProductRepository;
import JOO.jooshop.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final AdminProductRepository productRepository;

    /**
     * 상품 등록
     */
    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        Product product = dto.toEntity();
        productRepository.save(product);
        return ProductDto.fromEntity(product);
    }

    /**
     * 상품 수정
     */
    @Transactional
    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다. ID: " + id));
        product.updateFromDto(dto);
        return ProductDto.fromEntity(product);
    }

    /**
     * 전체 상품 조회
     */
    @Transactional(readOnly = true)
    public List<ProductDto> findAllProduct() {
        return productRepository.findAll()
                .stream()
                .map(ProductDto::fromEntity)  // 엔티티 → DTO 변환
                .collect(Collectors.toList());
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다. ID: " + id));
        productRepository.delete(product);
    }
}
