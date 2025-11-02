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

/*
 * 단순 DB 저장 + fileStorageService 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

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

    /** 상품별 썸네일 URL 반환 */
    @Transactional(readOnly = true)
    public List<String> getProductThumbnails(Long productId) {
        return productThumbnailRepository.findByProduct_ProductId(productId)
                .stream()
                .map(ProductThumbnail::getImagePath)
                .toList();
    }

    /** HTML 출력용 URL 반환 */
    @Transactional(readOnly = true)
    public List<String> getThumbnailUrls(Long productId) {
        return getProductThumbnails(productId)
                .stream()
                .map(path -> UPLOAD_PREFIX + path) // "/uploads/" + "thumbnails/abc.jpg"
                .collect(Collectors.toList());
    }

    /**
     * 특정 상품의 클라이언트 접근용 절대 URL 반환
     * DB에는 상대경로만 저장되어 있으므로, "/uploads/"를 붙여서 반환
     */
    @Transactional
    public void uploadThumbnailImages(Product product, MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        try {
            String imagePath = fileStorageService.saveFile(file, "thumbnails");
            ProductThumbnail thumbnail = ProductThumbnail.builder()
                    .product(product)
                    .imagePath(imagePath) // DB에는 상대 URL 저장
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
