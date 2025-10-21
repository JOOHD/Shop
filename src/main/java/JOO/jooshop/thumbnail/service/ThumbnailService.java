package JOO.jooshop.thumbnail.service;

import JOO.jooshop.global.authorization.RequiresRole;
import JOO.jooshop.global.file.FileStorageService;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.thumbnail.entity.ProductThumbnail;
import JOO.jooshop.thumbnail.repository.ProductThumbnailRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ThumbnailService {
    /* 25.10.21 리팩토링
     * 1. MultipartFile 관련 코드 제거 → 실무에서는 파일 서버(예: S3)에 이미 업로드된 URL만 저장.
     * 2. 메서드 이름과 역할 단순화 → uploadThumbnailImages(Product, String) 처럼 URL 처리용만 유지.
     * 3. 삭제와 조회는 그대로 유지 → DB와 파일 삭제는 필요.
     */

    private final FileStorageService fileStorageService;
    private final ProductThumbnailRepositoryV1 productThumbnailRepository;

    /* 썸네일 업로드 */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void uploadThumbnailImages(Product product, String thumbnailUrl) {
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) return;

        ProductThumbnail thumbnailImage = new ProductThumbnail(product, thumbnailUrl);
        productThumbnailRepository.save(thumbnailImage);
    }

    /* 모든 썸네일 전체 조회 */
    public List<String> getAllThumbnails() {
        return mapToImagePaths(productThumbnailRepository.findAll());
    }

    /* 특정 상품 썸네일 조회 */
    public List<String> getProductThumbnails(Long productId) {
        return mapToImagePaths(productThumbnailRepository.findByProductProductId(productId));
    }

    private List<String> mapToImagePaths(List<ProductThumbnail> thumbnails) {
        return thumbnails.stream()
                .map(ProductThumbnail::getImagePath)
                .collect(Collectors.toList());
    }
    // <List<ProductThumbnail>> -> List<String> stream 사용 시, 타입 변경
    // -> boot는 Jackson 을 사용해서 java 객체를 JSON으로 직렬화
//    public List<ProductThumbnail> getProductThumbnails(Long productId) {
//        return productThumbnailRepository.findByProductProductId(productId);
//    }

    /* 썸네일 삭제 */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void deleteThumbnail(Long thumbnailId) {
        ProductThumbnail thumbnail = productThumbnailRepository.findById(thumbnailId)
                .orElseThrow(() -> new NoSuchElementException("해당 사진을 찾을 수 없습니다."));

        // DB에서 삭제
        productThumbnailRepository.delete(thumbnail);

        // 실제 파일 삭제
        fileStorageService.deleteFile(thumbnail.getImagePath());
    }
}
