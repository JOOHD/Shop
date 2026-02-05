package JOO.jooshop.thumbnail.service;

import JOO.jooshop.global.file.FileStorageService;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import JOO.jooshop.thumbnail.model.ProductThumbnailDto;
import JOO.jooshop.thumbnail.repository.ProductThumbnailRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    /**
     * Thumbnail 저장 정책
     * - 운영: MultipartFile 업로드 -> DB에는 상대 경로 저장 (ex: "thumbnails/abc.jpg")
     * - 테스트/더미: 외부 절대 URL을 그대로 저장 (ex: "https://.../image.jpg")
     *
     * 렌더링 정책
     * - 외부 URL: 그대로 반환
     * - 로컬 상대 경로: "/uploads/" prefix를 붙여 반환
     */

    private final ProductThumbnailRepositoryV1 productThumbnailRepository;
    private final FileStorageService fileStorageService;

    private static final String UPLOAD_PREFIX = "/uploads/";

    /* =========================
       Query
    ========================= */

    /** 전체 썸네일 DTO 반환 */
    @Transactional(readOnly = true)
    public List<ProductThumbnailDto> getAllThumbnails() {
        return productThumbnailRepository.findAll()
                .stream()
                .map(ProductThumbnailDto::new)
                .collect(Collectors.toList());
    }

    /** 상품별 썸네일 raw path(DB 저장값 그대로) */
    @Transactional(readOnly = true)
    public List<String> getProductThumbnails(Long productId) {
        return productThumbnailRepository.findByProduct_ProductId(productId)
                .stream()
                .map(ProductThumbnail::getImagePath)
                .toList();
    }

    /** 상품별 썸네일 URL(클라이언트 렌더링용 변환 적용) */
    @Transactional(readOnly = true)
    public List<String> getThumbnailUrls(Long productId) {
        return getProductThumbnails(productId)
                .stream()
                .map(this::toClientUrl)
                .toList();
    }

    /**
     * ✅ 대표 썸네일 1개 반환 (productId 기반)
     * - 외부/로컬 모두 지원 (로컬은 /uploads/ 붙여 반환)
     */
    @Transactional(readOnly = true)
    public String getRepresentativeThumbnailUrl(Long productId) {
        return productThumbnailRepository.findByProduct_ProductId(productId)
                .stream()
                .map(ProductThumbnail::getImagePath)
                .map(this::toClientUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * ✅ 대표 썸네일 1개 반환 (Product 엔티티 기반, 추가 쿼리 없음)
     */
    public String pickRepresentativeThumbnailUrl(Product product) {
        if (product == null) return null;

        return product.getProductThumbnails().stream()
                .map(ProductThumbnail::getImagePath)
                .map(this::toClientUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    /* =========================
       Command (save)
    ========================= */

    /**
     * 로컬 업로드 기반 썸네일 저장
     * - 파일 저장 후 DB에는 상대 경로 저장
     */
    @Transactional
    public void uploadThumbnailImages(Product product, MultipartFile file) {
        if (product == null) throw new IllegalArgumentException("product must not be null");
        if (file == null || file.isEmpty()) return;

        try {
            String relativePath = fileStorageService.saveFile(file, "thumbnails"); // ex: "thumbnails/abc.jpg"

            // ✅ 엔티티 생성 규칙 통일
            ProductThumbnail thumbnail = ProductThumbnail.create(product, relativePath);

            // 컬렉션 일관성
            product.getProductThumbnails().add(thumbnail);

            // DB 저장
            productThumbnailRepository.save(thumbnail);

        } catch (IOException e) {
            log.error("썸네일 업로드 실패: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("썸네일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 테스트/더미용: 외부 URL 썸네일 저장
     */
    @Transactional
    public void addExternalThumbnail(Product product, String externalImageUrl) {
        if (product == null) throw new IllegalArgumentException("product must not be null");
        String normalized = normalizeExternalUrl(externalImageUrl);

        ProductThumbnail thumbnail = ProductThumbnail.create(product, normalized);

        product.getProductThumbnails().add(thumbnail);
        productThumbnailRepository.save(thumbnail);
    }

    /* =========================
       Command (delete)
    ========================= */

    /** 썸네일 1개 삭제 (파일까지 정리) */
    @Transactional
    public void deleteThumbnail(Long thumbnailId) {
        productThumbnailRepository.findById(thumbnailId).ifPresent(thumbnail -> {
            deleteFileIfLocal(thumbnail.getImagePath());

            // 컬렉션 정리(가능하면)
            try {
                Product product = thumbnail.getProduct();
                if (product != null) {
                    product.getProductThumbnails().remove(thumbnail);
                }
            } catch (Exception ignore) {
            }

            productThumbnailRepository.delete(thumbnail);
        });
    }

    /**
     * ✅ 상품의 썸네일 전체 삭제 (파일까지 정리)
     * - AdminProductService.deleteProduct에서 사용
     */
    @Transactional
    public void deleteAllThumbnailsByProductId(Long productId) {
        List<ProductThumbnail> thumbnails = productThumbnailRepository.findByProduct_ProductId(productId);

        for (ProductThumbnail t : thumbnails) {
            deleteFileIfLocal(t.getImagePath());
        }

        productThumbnailRepository.deleteAllInBatch(thumbnails);
    }

    /* =========================
       Utils
    ========================= */

    private void deleteFileIfLocal(String path) {
        if (!isLocalRelativePath(path)) return;

        try {
            fileStorageService.deleteFile(path);
        } catch (Exception e) {
            log.error("썸네일 파일 삭제 실패: {}", path, e);
        }
    }

    private String normalizeExternalUrl(String url) {
        if (url == null) throw new IllegalArgumentException("external url is null");

        String trimmed = url.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("외부 URL 썸네일 경로가 비어 있습니다.");

        if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            throw new IllegalArgumentException("외부 URL 썸네일만 허용됩니다.");
        }

        return trimmed;
    }

    /** DB 저장값을 클라이언트 렌더링용 URL로 변환 */
    private String toClientUrl(String path) {
        if (path == null) return null;

        String trimmed = path.trim();
        if (trimmed.isBlank()) return null;

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;

        if (trimmed.startsWith(UPLOAD_PREFIX)) return trimmed;

        return UPLOAD_PREFIX + trimmed;
    }

    /** 로컬 상대경로 여부 */
    private boolean isLocalRelativePath(String path) {
        if (path == null) return false;
        String t = path.trim();
        if (t.isBlank()) return false;
        return !(t.startsWith("http://") || t.startsWith("https://"));
    }
}
