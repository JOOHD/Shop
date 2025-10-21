package JOO.jooshop.contentImgs.service;

import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.contentImgs.repository.ContentImagesRepository;
import JOO.jooshop.global.file.FileStorageService;
import JOO.jooshop.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ContentImgService {

    private final FileStorageService fileStorageService;
    private final ContentImagesRepository contentImagesRepository;

    /*  파일 저장 책임 분리 (실무)
        - 실제 이미지 파일 업로드는 프론트엔드(예: AWS S3 SDK, CloudFront 등)나 별도 파일 서버에서 처리함.
        - 백엔드는 이미 업로드된 URL만 저장해서 책임이 단순해짐.
    */
    public void uploadContentImages(Product product, List<String> contentUrls, UploadType uploadType) {
        if (contentUrls == null || contentUrls.isEmpty()) return;

        for (String contentUrl : contentUrls) {
            if (contentUrl == null || contentUrl.isBlank()) continue;
            ContentImages contentImage = new ContentImages(product, contentUrl, uploadType);
            contentImagesRepository.save(contentImage);
        }
    }
    /*  상세 이미지 업로드 (개발/테스트용)
        - 서버가 직접 파일을 받아서 local/storage 저장
        - MultipartFile 로 업로드 후, URL 생성

    public void uploadContentImages(Product product, List<MultipartFile> images, UploadType uploadType) {
        if (images == null || images.isEmpty()) return;

        for (MultipartFile image : images) {
            try {
                String dbFileUrl = fileStorageService.saveFileUrl(image, uploadType.getDbPath());
                ContentImages contentImage = new ContentImages(product, dbFileUrl, uploadType);
                contentImagesRepository.save(contentImage);
            } catch (IOException e) {
                log.error("상세 이미지 업로드 실패: {}", image.getOriginalFilename(), e);
                throw new RuntimeException("상세 이미지 업로드 중 오류가 발생했습니다.", e);
            }
        }
    }
    */

    /* 특정 상품 이미지 조회 */
    public List<ContentImages> getContentImages(Long productId) {
        return contentImagesRepository.findByProductProductId(productId);
    }

    /* 상세 이미지 삭제 */
    @Transactional
    public void deleteContentImage(Long contentImgId) {
        ContentImages contentImage = contentImagesRepository.findById(contentImgId)
                .orElseThrow(() -> new NoSuchElementException("해당 사진을 찾을 수 없습니다."));

        contentImagesRepository.delete(contentImage);
        fileStorageService.deleteFile(contentImage.getImagePath());
    }
}
