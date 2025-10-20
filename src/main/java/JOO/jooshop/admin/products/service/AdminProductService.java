package JOO.jooshop.admin.products.service;

import JOO.jooshop.admin.products.model.AdminProductRequestDto;
import JOO.jooshop.admin.products.model.AdminProductResponseDto ;
import JOO.jooshop.admin.products.repository.AdminProductRepository;
import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.contentImgs.service.ContentImgService;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.thumbnail.service.ThumbnailServiceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProductService {

    private final AdminProductRepository productRepository;
    private final ThumbnailServiceV1 thumbnailService;
    private final ContentImgService contentImgService;

    /** 전체 상품 조회 */
    public List<AdminProductResponseDto> findAllProduct() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /** 상품 등록 */
    public AdminProductResponseDto createProduct(AdminProductRequestDto dto) {
        Product product = new Product(dto);
        Product saved = productRepository.save(product);

        // 이미지 + 옵션 처리
        handleImagesAndOptions(saved, dto);

        return toResponseDto(saved);
    }

    /** 상품 수정 */
    public AdminProductResponseDto updateProduct(Long id, AdminProductRequestDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다."));

        product.updateFromDto(dto);

        // 이미지 + 옵션 처리
        handleImagesAndOptions(product, dto);

        return toResponseDto(product);
    }

    /** 상품 삭제 */
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    /** 이미지 + 옵션 처리 메서드 */
    private void handleImagesAndOptions(Product product, AdminProductRequestDto dto) {
        // 1. 썸네일 등록 (front에서 URL 전달 시)
        if (dto.getThumbnailUrl() != null && !dto.getThumbnailUrl().isBlank()) {
            // List<String>이 아니라 단일 URL일 경우, 내부에서 List로 감싸서 처리하도록
            thumbnailService.uploadThumbnail(product, dto.getThumbnailUrl());
        }

        // 2. 상세 이미지 등록
        if (dto.getContentUrls() != null && !dto.getContentUrls().isEmpty()) {
            contentImgService.registerContentImages(product, dto.getContentUrls(), UploadType.PRODUCT);
        }

        // 3. 옵션 업데이트
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            product.updateProductManagements(dto.getOptions()); // Product 엔티티 안에서 처리
        }
    }

    /** Response DTO 변환 */
    private AdminProductResponseDto toResponseDto(Product product) {
        return new AdminProductResponseDto(
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

