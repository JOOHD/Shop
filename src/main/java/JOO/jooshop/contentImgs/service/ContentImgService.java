package JOO.jooshop.contentImgs.service;

import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.contentImgs.repository.ContentImagesRepository;
import JOO.jooshop.product.entity.Product;
import lombok.RequiredArgsConstructor;
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

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ContentImgService {

    private final ContentImagesRepository contentImagesRepository;

    /**
     * 범용 이미지 업로드 메서드
     * @param product 업로드할 상품
     * @param images 업로드 파일 리스트
     * @param uploadType 업로드 타입(enum)
     */
    public void uploadContentImage(Product product, List<MultipartFile> images, UploadType uploadType) {
        try {
            // 업로드 타입에 맞는 로컬 저장 경로
            String uploadsDir = uploadType.getLocalPath();

            for (MultipartFile image : images) {
                // DB 저장 경로 생성
                String dbFilePath = saveContentImage(image, uploadsDir, uploadType.getDbPath());

                // 엔티티 생성 후 저장
                ContentImages contentImages = new ContentImages(product, dbFilePath);
                contentImagesRepository.save(contentImages);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String saveContentImage(MultipartFile image, String uploadsDir, String dbBasePath) throws IOException {
        String fileName = UUID.randomUUID().toString().replace("-", "") + "_" + image.getOriginalFilename();
        String filePath = uploadsDir + fileName;
        String dbFilePath = dbBasePath + fileName;

        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, image.getBytes());

        return dbFilePath;
    }

    public void deleteContentImage(Long contentImgId) {
        ContentImages contentImage = contentImagesRepository.findById(contentImgId)
                .orElseThrow(() -> new NoSuchElementException("해당 사진을 찾을 수 없습니다."));

        String imagePath = "src/main/resources/static" + contentImage.getImagePath();

        contentImagesRepository.delete(contentImage);
        deleteImageFile(imagePath);
    }

    private void deleteImageFile(String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ContentImages> getContentImgs(Long productId) {
        return contentImagesRepository.findByProduct_ProductId(productId);
    }
}
