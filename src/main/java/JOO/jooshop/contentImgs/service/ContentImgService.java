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
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ContentImgService {

    private final FileStorageService fileStorageService;
    private final ContentImagesRepository contentImagesRepository;

    private static final String CONTENT_IMG_DIR = "contentImgs"; // 하위 폴더

    /**
     * MultipartFile로 업로드
     */
    public void uploadContentImages(Product product, List<MultipartFile> images, UploadType uploadType) {
        if (images == null || images.isEmpty()) return;

        for (MultipartFile image : images) {
            try {
                String dbFileUrl = fileStorageService.saveFile(image, CONTENT_IMG_DIR);
                ContentImages contentImage = new ContentImages(product, dbFileUrl, uploadType);
                contentImagesRepository.save(contentImage);
            } catch (IOException e) {
                log.error("상세 이미지 업로드 실패: {}", image.getOriginalFilename(), e);
                throw new RuntimeException("상세 이미지 업로드 중 오류가 발생했습니다.", e);
            }
        }
    }

    /**
     * 특정 상품 이미지 조회
     */
    public List<ContentImages> getContentImages(Long productId) {
        return contentImagesRepository.findByProductProductId(productId);
    }

    /**
     * 상세 이미지 삭제
     */
    public void deleteContentImage(Long contentImgId) {
        ContentImages contentImage = contentImagesRepository.findById(contentImgId)
                .orElseThrow(() -> new NoSuchElementException("해당 사진을 찾을 수 없습니다."));

        contentImagesRepository.delete(contentImage);
        fileStorageService.deleteFile(contentImage.getImagePath());
    }
}
