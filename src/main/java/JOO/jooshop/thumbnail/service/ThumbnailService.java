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

    /** 전체 상품 + DTO 반환 (뷰용) */
    @Transactional(readOnly = true)
    public List<ProductThumbnailDto> getAllThumbnails() {
        return productThumbnailRepository.findAll()
                .stream()
                .map(ProductThumbnailDto::new)
                .toList();
    }

    /** 상품별 썸네일 URL 반환 */
    @Transactional(readOnly = true)
    public List<String> getProductThumbnails(Long productId) {
        return productThumbnailRepository.findByProduct_ProductId(productId)
                .stream()
                .map(ProductThumbnail::getImagePath)
                .toList();
    }

    /**
     * 썸네일 이미지 업로드 및 저장 (파일 업로드 전용)
     */
    @Transactional
    public void uploadThumbnailImages(Product product, MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        try {
            // 파일 저장 (하위 디렉토리: "thumbnails")
            String imageUrl = fileStorageService.saveFile(file, "thumbnails");

            // DB 저장
            ProductThumbnail thumbnail = ProductThumbnail.builder()
                    .product(product)
                    .imagePath(imageUrl)
                    .build();

            productThumbnailRepository.save(thumbnail);
        } catch (IOException e) {
            log.error("썸네일 업로드 실패: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("썸네일 업로드 중 오류가 발생했습니다.");
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
}
