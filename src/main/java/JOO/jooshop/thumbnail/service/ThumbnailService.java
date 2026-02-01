package JOO.jooshop.thumbnail.service;

import JOO.jooshop.global.file.FileStorageService;
import JOO.jooshop.global.image.ImageUtil;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import JOO.jooshop.thumbnail.model.ProductThumbnailDto;
import JOO.jooshop.thumbnail.repository.ProductThumbnailRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    /*
     * Thumbnail 저장 정책
     * - 운영: MultipartFile 업로드 -> DB에는 상대 경로 저장 (ex: "thumbnails/abc.jpg")
     * - 테스트/더미: 외부 절대 URL을 그대로 저장 (ex: "https://.../image.jpg")
     *
     * 핵심:
     * - "외부 URL만 허용" 검증은 외부 URL 저장 메서드에서만 수행한다.
     *   (MultipartFile 업로드 경로는 당연히 http로 시작하지 않으므로 동일 검증을 걸면 운영 기능이 깨짐)
     */

    private final ProductThumbnailRepositoryV1 productThumbnailRepository;
    private final FileStorageService fileStorageService;

    private static final String UPLOAD_PREFIX = "/uploads/";

    /** 전체 상품 + DTO 반환 (뷰용) */
    @Transactional(readOnly = true)
    public List<ProductThumbnailDto> getAllThumbnails() {
        return productThumbnailRepository.findAll()
                .stream()
                .map(ProductThumbnailDto::new)
                .collect(Collectors.toList());
    }

    /** 상품별 썸네일 경로 반환 (DB 저장값 그대로: 상대경로 or 외부URL) */
    @Transactional(readOnly = true)
    public List<String> getProductThumbnails(Long productId) {
        return productThumbnailRepository.findByProduct_ProductId(productId)
                .stream()
                .map(ProductThumbnail::getImagePath)
                .toList();
    }

    /** /**
     * HTML 출력용 URL 반환
      (로컬 업로드 상대경로 -> "/uploads/" prefix 붙여서 반환)
     *
     * 주의:
     * - DB에 외부 URL이 섞여있을 수 있으므로
     *   외부 URL은 prefix를 붙이지 않고 그대로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<String> getThumbnailUrls(Long productId) {
        return getProductThumbnails(productId)
                .stream()
                .map(this::toClientUrl)
                .collect(Collectors.toList());
    }

    /**
     * 로컬 업로드 기반 썸네일 저장
     * fileStorageService 파일 저장
     * DB에는 상대 경로만 저장 (ex: "thumbnail/abc.jpg)
     */
    @Transactional
    public void uploadThumbnailImages(Product product, MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        try {
            String imagePath = fileStorageService.saveFile(file, "thumbnails");
            
            ProductThumbnail thumbnail = ProductThumbnail.builder()
                    .product(product)
                    .imagePath(imagePath) // DB에는 상대경로 저장
                    .build();

            // 양방향 연관관계 유지
            product.addThumbnail(thumbnail);
            
            productThumbnailRepository.save(thumbnail);

        } catch (IOException e) {
            log.error("썸네일 업로드 실패: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("썸네일 업로드 중 오류가 발생했습니다.");
        }
    }

    /**
     * 테스트/더미용: 외부 URL 썸네일 저장
     * 외부 URL만 허용 (검증 적용 시점)
     * DB에는 외부 절대 URL 그대로 저장
     */
    @Transactional
    public void addExternalThumbnail(Product product, String externalImageUrl) {
        validateExternalThumbnailUrl(externalImageUrl);

        ProductThumbnail thumbnail = ProductThumbnail.builder()
                .product(product)
                .imagePath(externalImageUrl) // DB에 외부 URL 그대로 저장
                .build();

        product.addThumbnail(thumbnail);
        productThumbnailRepository.save(thumbnail);
    }

    /** 썸네일 삭제 */
    @Transactional
    public void deleteThumbnail(Long thumbnailId) {
        productThumbnailRepository.findById(thumbnailId).ifPresent(thumbnail -> {

            // 로컬 파일인 경우에만 실제 파일 삭제 시도
            // (외부 URL은 파일 시스템에 없으니 DELETE 하면 안 됨)
            try {
                if (isLocalRelativePath(thumbnail.getImagePath())) {
                    fileStorageService.deleteFile(thumbnail.getImagePath());
                }
            } catch (Exception e) {
                log.error("썸네일 파일 삭제 실패: {}", thumbnail.getImagePath(), e);
            }
            productThumbnailRepository.delete(thumbnail);
        });
    }

    // =========================
    // 내부 유틸 / 검증
    // =========================

    /** 외부 URL 썸네일만 허용 (더미/테스트용 저장 경로에서만 사용) */
    private void validateExternalThumbnailUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            throw new IllegalArgumentException("외부 URL 썸네일 경로가 비어 있습니다.");
        }
        if (!imagePath.startsWith("http")) {
            throw new IllegalArgumentException("외부 URL 썸네일만 허용됩니다.");
        }
    }

    /** DB 저장값을 클라이언트용 URL로 변환 */
    private String toClientUrl(String path) {
        if (path == null || path.isBlank()) return null;

        // 외부 URL이면 그대로
        if (path.startsWith("http")) return path;

        // 로컬 상대경로면 "/uploads/" prefix 붙여서 반환
        return UPLOAD_PREFIX + path;
    }

    /** 로컬 상대경로 여부 판단 (파일시스템 삭제 대상인지 판별) */
    private boolean isLocalRelativePath(String path) {
        return path != null && !path.startsWith("http");
    }
}
