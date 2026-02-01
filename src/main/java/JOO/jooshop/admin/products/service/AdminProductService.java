package JOO.jooshop.admin.products.service;

import JOO.jooshop.admin.products.model.AdminProductRequestDto;
import JOO.jooshop.admin.products.model.AdminProductResponseDto ;
import JOO.jooshop.admin.products.repository.AdminProductRepository;
import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.contentImgs.service.ContentImgService;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
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

    private final ThumbnailService thumbnailService;
    private final ContentImgService contentImgService;
    private final AdminProductRepository productRepository;

    /** 전체 상품 조회 */
    public List<AdminProductResponseDto> findAllProduct() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponseDto) // DB Product -> 화면용 DTO 변환
                .collect(Collectors.toList());
    }

    /** DTO 변환
     *
     * 리팩토링 히스토리
     * - 기존 구현은 productThumbnails.get(0)을 대표 썸네일로 사용
     * - @OneToMany List는 순서가 보장되지 않기 때문에(정렬 규칙 미정),
     *   썸네일 데이터가 혼재(예: 로컬 상대경로 + 외부 URL)되면 대표 이미지가 의도와 다르게 선택될 수 있음
     *
     * 개선 내용
     * - 관리자 목록에서는 "외부 URL 기반 대표 썸네일"을 우선 렌더링하도록 정책을 명시
     * - path.startsWith("http")인 데이터만 후보로 선택해 화면 안정성 확보
     *
     * 참고
     * - 운영 썸네일(로컬 업로드)은 상대경로로 저장되므로,
     *   필요 시 ThumbnailService.toClientUrl() 같은 변환 정책으로 확장 가능
     */
    private AdminProductResponseDto toResponseDto(Product product) {
        String thumbnailUrl = product.getProductThumbnails().stream()
                .map(ProductThumbnail::getImagePath)
                .filter(path -> path != null && path.startsWith("http")) // 외부 URL 만 대표 썸네일로 사용
                .findFirst()
                .orElse(null);

        return new AdminProductResponseDto(
                product.getProductId(),
                product.getProductName(),
                product.getProductType(),
                product.getPrice(),
                product.getDiscountRate(),
                product.getProductInfo(),
                thumbnailUrl,
                product.getCreatedAt()
        );
    }

    /** 상품 등록 */
    public AdminProductResponseDto createProduct(AdminProductRequestDto dto,
                                                 MultipartFile thumbnail,
                                                 List<MultipartFile> contentImages) {
        Product product = new Product(dto);

        // 옵션 처리
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            product.updateProductManagements(dto.getOptions());
        }

        // Product + ProductManagement 모두 저장
        Product saved = productRepository.save(product);

        // 이미지 처리
        if (thumbnail != null && !thumbnail.isEmpty()) {
            thumbnailService.uploadThumbnailImages(saved, thumbnail);
        }
        if (contentImages != null && !contentImages.isEmpty()) {
            contentImgService.uploadContentImages(saved, contentImages, UploadType.PRODUCT);
        }

        // 저장 직후 returned DTO는 "외부 URL 대표 썸네일" 정책에 의해 null일 수 있음
        // (운영 로컬 상대경로를 대표로 쓰려면 thumbnailUrl 계산 정책을 확장해야 함)
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

        // 1. 썸네일 업로드 (로컬 업로드)
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

}

