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

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final ProductThumbnailRepositoryV1 productThumbnailRepository;
    private final FileStorageService fileStorageService;

    private static final String THUMBNAIL_DIR = "thumbnails"; // 저장 폴더

    /** 썸네일 업로드 (MultipartFile 적용) */
    @Transactional
    public void uploadThumbnailImages(Product product, MultipartFile thumbnailFile) {
        if (thumbnailFile == null || thumbnailFile.isEmpty()) {
            throw new IllegalArgumentException("썸네일 파일이 비어있습니다.");
        }

        try {
            // 파일 저장
            String relativePath = fileStorageService.saveFile(thumbnailFile, THUMBNAIL_DIR);

            // 엔티티 저장
            ProductThumbnail thumbnail = new ProductThumbnail(product, relativePath);
            productThumbnailRepository.save(thumbnail);

        } catch (IOException e) {
            log.error("썸네일 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("썸네일 업로드 실패", e);
        }
    }

    /** 썸네일 삭제 */
    @Transactional
    public void deleteThumbnail(Long thumbnailId) {
        productThumbnailRepository.findById(thumbnailId).ifPresent(thumbnail -> {
            try {
                fileStorageService.deleteFile(thumbnail.getImagePath());
            } catch (Exception e) {
                log.error("썸네일 파일 삭제 실패: {}", thumbnail.getImagePath(), e);
            }
            productThumbnailRepository.delete(thumbnail);
        });
    }

    /** 상품별 썸네일 URL 반환 */
    @Transactional(readOnly = true)
    public List<String> getProductThumbnails(Long productId) {
        return productThumbnailRepository.findByProductProductId(productId)
                .stream()
                .map(ProductThumbnail::getImagePath)
                .toList();
    }

    /** 전체 상품 + DTO 반환 (뷰용) */
    @Transactional(readOnly = true)
    public List<ProductThumbnailDto> getAllThumbnails() {
        return productThumbnailRepository.findAll()
                .stream()
                .map(ProductThumbnailDto::new)
                .toList();
    }
}
