package JOO.jooshop.contentImgs.service;

import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.contentImgs.repository.ContentImagesRepository;
import JOO.jooshop.global.file.FileStorageService;
import JOO.jooshop.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ContentImgService {

    /**
    지금 코드에서 고쳐야 할 핵심

    1. new ContentImages(product, dbFilePath, uploadType)
        → 우리가 엔티티를 create() 팩토리로 통일했으니 Factory 사용으로 바꿔야 함

    2. Product 컬렉션(contentImages)에 추가를 안 함
        → orphanRemoval=true라면 “부모 컬렉션”과도 일관되게 맞추는 게 최선

    3. delete 시 DB 삭제 후 파일 삭제 순서가 위험
        - 지금은 delete(contentImage) 먼저 하고 그 다음에 deleteFile(...)인데
        - 파일 삭제 실패하면 DB는 이미 삭제돼서 “실제 파일만 남는” 케이스가 생김
            ✅ 최선은 파일 삭제 먼저(try/catch) + DB 삭제는 반드시 진행

    4. 이미지 업로드에서 product null 방어 없음

    4. 여러 장 업로드는 saveAll로 성능 개선 가능 (선택)
    */

    private final FileStorageService fileStorageService;
    private final ContentImagesRepository contentImagesRepository;

    private static final String CONTENT_IMG_DIR = "contentImgs";

    /** MultipartFile 업로드 및 DB 저장 */
    public void uploadContentImages(Product product, List<MultipartFile> images, UploadType uploadType) {
        if (product == null) throw new IllegalArgumentException("product must not be null");
        if (uploadType == null) throw new IllegalArgumentException("uploadType must not be null");
        if (images == null || images.isEmpty()) return;

        List<ContentImages> toSave = new ArrayList<>();

        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) continue;

            try {
                String relativePath = fileStorageService.saveFile(image, CONTENT_IMG_DIR);

                // ✅ 엔티티 생성 규칙 통일
                ContentImages contentImage = ContentImages.create(product, relativePath, uploadType);

                // ✅ 부모 컬렉션에도 반영(일관성)
                product.getContentImages().add(contentImage);

                toSave.add(contentImage);
            } catch (IOException e) {
                log.error("상세 이미지 업로드 실패: {}", image.getOriginalFilename(), e);
                throw new RuntimeException("상세 이미지 업로드 중 오류가 발생했습니다.", e);
            }
        }

        if (!toSave.isEmpty()) {
            // ✅ 성능: 여러 장이면 saveAll
            contentImagesRepository.saveAll(toSave);
        }
    }

    /** 특정 상품의 이미지 리스트 */
    @Transactional(readOnly = true)
    public List<ContentImages> getContentImages(Long productId) {
        return contentImagesRepository.findByProduct_ProductId(productId);
    }

    /** 상세 이미지 삭제 */
    public void deleteContentImage(Long contentImgId) {
        ContentImages contentImage = contentImagesRepository.findById(contentImgId)
                .orElseThrow(() -> new NoSuchElementException("해당 사진을 찾을 수 없습니다."));

        String path = contentImage.getImagePath();

        // 1) 파일 삭제 먼저 시도 (실패해도 DB 삭제는 진행)
        try {
            fileStorageService.deleteFile(path);
        } catch (Exception e) {
            log.error("상세 이미지 파일 삭제 실패: {}", path, e);
        }

        // 2) 영속성 컨텍스트에서 부모 컬렉션도 정리(가능하면)
        try {
            Product product = contentImage.getProduct();
            if (product != null) {
                product.getContentImages().remove(contentImage);
            }
        } catch (Exception ignore) {
        }

        // 3) DB 삭제
        contentImagesRepository.delete(contentImage);
    }
}
