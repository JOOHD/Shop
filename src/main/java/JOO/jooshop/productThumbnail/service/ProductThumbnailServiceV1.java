package JOO.jooshop.productThumbnail.service;

import JOO.jooshop.global.authorization.RequiresRole;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productThumbnail.entity.ProductThumbnail;
import JOO.jooshop.productThumbnail.repository.ProductThumbnailRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductThumbnailServiceV1 {

    /*
        1. Product 객체와 MultipartFile 타입의 리스트를 파라미터로 받는다.
        2. 사진을 저장할 경로 생성, 클라이언트에서 경로로 접근해 파일을 찾을 때,
            static 아래에서 찾기 위해 경로 설정
     */
    private final ProductThumbnailRepositoryV1 productThumbnailRepository;
    private final ProductRepositoryV1 productRepository;

    /**
     * 썸네일 등록
     * @param product
     * @param images
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void uploadThumbnail(Product product, List<MultipartFile> images) {
        try {
            // 이미지 파일 저장을 위한 경로 설정
            String uploadsDir = "src/main/resources/static/uploads/thumbnails/";

            // 각 이미지 파일에 대해 업로드 및 DB 저장 수행
            for (MultipartFile image : images) {

                // 이미지 파일 경로를 저장
                String dbFilePath = saveImage(image, uploadsDir);

                // ProductThumbnail 엔티티 생성 및 저장
                ProductThumbnail thumbnail = new ProductThumbnail(product, dbFilePath);
                productThumbnailRepository.save(thumbnail);
            }
        } catch (IOException e) {
            // 파일 저장 중 오류가 발생한 경우 처리
            e.printStackTrace();
        }
    }

    /**
     * @param image
     * @param uploadsDir
     * @return
     * @throws IOException
     * 
     * 지금 코드는 상대경로로 지정하였지만, JAR로 패키징 후엔 디렉토리가 읽기 전용이 되기 때문에 업로드 실패
     * -> file: upload-dir : C:/jooshop/uploads/ (절대경로) 외부 저장
     * -> file: upload-dir : src/main/resources/static.. (상대경로) 프로젝트 내부 저장, 읽기 전용
     */
    
    private String saveImage(MultipartFile image, String uploadsDir) throws IOException {
        // 파일 이름 생성
        String fileName = UUID.randomUUID().toString().replace("-", "") + "_" + image.getOriginalFilename();
        log.info("*************OriginFileName check" + image.getOriginalFilename());
        log.info("*************fileName check" + fileName);
        // 실제 파일이 저장될 경로
        String filePath = uploadsDir + fileName;
        log.info("*************filePath check" + filePath);
        // DB에 저장할 경로 문자열
        String dbFilePath = "uploads/thumbnails/" + fileName;

        Path path = Paths.get(filePath); // Path 객체 생성
        Files.createDirectories(path.getParent()); // 디렉토리 생성
        Files.write(path, image.getBytes()); // 디렉토리에 파일 저장

        return dbFilePath;
    }

    /**
     * 썸네일 삭제
     * @param thumbnailId
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void deleteThumbnail(Long thumbnailId) {
        // 썸네일 엔티티 조회
        ProductThumbnail thumbnail = productThumbnailRepository.findById(thumbnailId)
                .orElseThrow(() -> new NoSuchElementException("해당 사진을 찾을 수 없습니다."));

        // 썸네일 파일 경로 가져오기
        String imagePath = "src/main/resources/static" + thumbnail.getImagePath();

        // 썸네일 데이터베이스에서 삭제
        productThumbnailRepository.delete(thumbnail);

        // 파일 삭제
        deleteImageFile(imagePath);
    }
    // 파일 삭제 메서드
    private void deleteImageFile(String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // 파일 삭제 중 오류 발생 시 예외 처리
            e.printStackTrace();
        }
    }

    //썸네일 조회
    public List<ProductThumbnail> getProductThumbnails(Long productId) {
        return productThumbnailRepository.findByProduct_ProductId(productId);
    }

    public List<Product> getAllProductsWithThumbnails() {
        return productRepository.findAll();
    }
}
