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
import java.util.*;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ContentImgService {

    private final ContentImagesRepository contentImagesRepository;

    /* 상세 이미지 업로드 */
    public void uploadContentImage(Product product, List<MultipartFile> images, UploadType uploadType) {
        if (images == null || images.isEmpty()) return;

        try {
            String uploadsDir = uploadType.getLocalPath();
            for (MultipartFile image : images) {
                String dbFilePath = saveContentImage(image, uploadsDir, uploadType.getDbPath());
                ContentImages contentImages = new ContentImages(product, dbFilePath, uploadType);
                contentImagesRepository.save(contentImages);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* 상세 이미지 URL 등록 (front 에서 직접 URL 전달 시) */
    public void registerContentImages(Product product, List<String> urls, UploadType uploadType) {
        if (urls == null || urls.isEmpty()) return;
        for (String url : urls) {
            if (url == null || url.isBlank()) continue;
            ContentImages contentImages = new ContentImages(product, url, uploadType);
            contentImagesRepository.save(contentImages);
        }
    }

    /* 특정 상품 이미지 조회 */
    public List<ContentImages> getContentImages(Long productId) {
        return contentImagesRepository.findByProductProductId(productId);
    }

    /* 실제 파일 저장 */
    private String saveContentImage(MultipartFile image, String uploadsDir, String dbBasePath) throws IOException {
        if (image == null || image.isEmpty()) return null;

        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) return null;

        String fileName = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
        String filePath = uploadsDir + fileName;
        String dbFilePath = dbBasePath + fileName;

        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, image.getBytes());

        return dbFilePath;
    }

    /* 상세 이미지 삭제 */
    @Transactional
    public void deleteContentImage(Long contentImgId) {
        ContentImages contentImage = contentImagesRepository.findById(contentImgId)
                .orElseThrow(() -> new NoSuchElementException("해당 사진을 찾을 수 없습니다."));

        // 파일 삭제
        String imagePath = "src/main/resources/static" + contentImage.getImagePath();
        deleteImageFile(imagePath);

        // DB 삭제
        contentImagesRepository.delete(contentImage);
    }

    // 이미지 파일
    private void deleteImageFile(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return;

        try {
            Path path = Paths.get(imagePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
