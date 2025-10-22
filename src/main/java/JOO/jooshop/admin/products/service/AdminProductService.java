package JOO.jooshop.admin.products.service;

import JOO.jooshop.admin.products.model.AdminProductRequestDto;
import JOO.jooshop.admin.products.model.AdminProductResponseDto ;
import JOO.jooshop.admin.products.repository.AdminProductRepository;
import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.contentImgs.service.ContentImgService;
import JOO.jooshop.global.file.FileStorageService;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.thumbnail.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminProductService {

    private final FileStorageService fileStorageService;
    private final ThumbnailService thumbnailService;
    private final ContentImgService contentImgService;
    private final AdminProductRepository productRepository;

    /** 전체 상품 조회 */
    public List<AdminProductResponseDto> findAllProduct() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /** 상품 등록 */
    public AdminProductResponseDto createProduct(AdminProductRequestDto dto,
                                                 MultipartFile thumbnail,
                                                 List<MultipartFile> contentImages) {
        Product product = new Product(dto);
        Product saved = productRepository.save(product);

        // 이미지 + 옵션 처리
        handleImagesAndOptions(saved, thumbnail, contentImages, dto.getOptions());

        return toResponseDto(saved);
    }

    /** 상품 수정 */
    public AdminProductResponseDto updateProduct(Long id,
                                                 AdminProductRequestDto dto,
                                                 MultipartFile thumbnail,
                                                 List<MultipartFile> contentImages) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다."));

        product.updateFromDto(dto);

        // 이미지 + 옵션 처리
        handleImagesAndOptions(product, thumbnail, contentImages, dto.getOptions());

        return toResponseDto(product);
    }

    /** 이미지 + 옵션 처리 메서드 (MultipartFile 버전) */
    private void handleImagesAndOptions(Product product,
                                        MultipartFile thumbnail,
                                        List<MultipartFile> contentImages,
                                        List<AdminProductRequestDto.ProductManagementDto> options) {

        // 1. 썸네일 업로드
        if (thumbnail != null && !thumbnail.isEmpty()) {
            thumbnailService.uploadThumbnailImages(product, thumbnail);
        }

        // 2. 상세 이미지 업로드
        if (contentImages != null && !contentImages.isEmpty()) {
            contentImgService.uploadContentImages(product, contentImages, UploadType.PRODUCT);
        }

        // 3. 옵션 업데이트
        if (options != null && !options.isEmpty()) {
            product.updateProductManagements(options);
        }
    }


    /** 상품 삭제 */
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
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

