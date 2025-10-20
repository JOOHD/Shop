package JOO.jooshop.thumbnail.service;

import JOO.jooshop.global.authorization.RequiresRole;
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
public class ThumbnailServiceV1 {

    private final ProductThumbnailRepositoryV1 productThumbnailRepository;

    /* 썸네일 업로드 */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void uploadThumbnail(Product product, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return;

        String uploadsDir = "src/main/resources/static/uploads/thumbnails/";

        for (MultipartFile image : images) {
            try {
                String dbFilePath = saveThumbnail(image, uploadsDir);
                ProductThumbnail thumbnail = new ProductThumbnail(product, dbFilePath);
                productThumbnailRepository.save(thumbnail);
            } catch (IOException e) {
                log.error("썸네일 업로드 실패: {}", image.getOriginalFilename(), e);
                throw new RuntimeException("썸네일 업로드 중 오류가 발생했습니다.");
            }
        }
    }

    private String saveThumbnail(MultipartFile image, String uploadDir) throws IOException {
        String fileName = UUID.randomUUID().toString().replace("-", "") + "_" + image.getOriginalFilename();
        String filePath = uploadDir + fileName;
        String dbFilePath = "uploads/thumbnails/" + fileName;

        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, image.getBytes());

        return dbFilePath;
    }

    /* 썸네일 조회 */
    public List<String> getProductThumbnails(Long productId) {
        return productThumbnailRepository.findByProductProductId(productId)
                .stream()
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

        String imagePath = "src/main/resources/static" + thumbnail.getImagePath();
        productThumbnailRepository.delete(thumbnail);
        deleteImageFile(imagePath);
    }
    // 파일 삭제 메서드
    private void deleteImageFile(String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", imagePath, e);
            throw new RuntimeException("썸네일 파일 삭제 실패.");
        }
    }
}
