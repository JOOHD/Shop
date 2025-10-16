package JOO.jooshop.admin.products.service;

import JOO.jooshop.admin.products.model.AdminProductEntityMapperDto;
import JOO.jooshop.admin.products.model.AdminProductRequestDto;
import JOO.jooshop.admin.products.model.AdminProductResponseDto ;
import JOO.jooshop.admin.products.repository.AdminProductRepository;
import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.contentImgs.service.ContentImgService;
import JOO.jooshop.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProductService {

    private final AdminProductRepository productRepository;
    private final ContentImgService contentImgService;

    /**
     * 전체 상품 조회
     */
    public List<AdminProductResponseDto> findAllProduct() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 상품 등록
     */
    public AdminProductResponseDto createProduct(AdminProductRequestDto dto, List<MultipartFile> images) {
        Product product = new Product(dto);
        Product saved = productRepository.save(product);

        if (images != null && !images.isEmpty()) {
            contentImgService.uploadContentImage(saved, images, UploadType.PRODUCT);
        }

        return toResponseDto(saved);
    }

    /**
     * 상품 수정
     */
    public AdminProductResponseDto updateProduct(Long id, AdminProductEntityMapperDto dto, List<MultipartFile> images) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다."));

        product.updateFromDto(dto);

        if (images != null && !images.isEmpty()) {
            contentImgService.uploadContentImage(product, images, UploadType.PRODUCT);
        }
        return toResponseDto(product);
    }

    /**
     * 상품 삭제
     */
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    private AdminProductResponseDto toResponseDto(Product product) {
        return new AdminProductResponseDto (
                product.getProductId(),
                product.getProductName(),
                product.getProductType(),
                product.getPrice(),
                product.getProductInfo(),
                product.getManufacturer(),
                product.getIsDiscount(),
                product.getDiscountRate(),
                product.getIsRecommend()
        );
    }
}
