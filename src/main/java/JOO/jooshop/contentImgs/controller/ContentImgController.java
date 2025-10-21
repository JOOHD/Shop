package JOO.jooshop.contentImgs.controller;

import JOO.jooshop.contentImgs.entity.ContentImages;
import JOO.jooshop.contentImgs.entity.enums.UploadType;
import JOO.jooshop.contentImgs.service.ContentImgService;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static JOO.jooshop.global.exception.ResponseMessageConstants.PRODUCT_NOT_FOUND;
import static JOO.jooshop.global.exception.ResponseMessageConstants.DELETE_SUCCESS;

@RestController
@RequestMapping("/api/v1/product/image")
@RequiredArgsConstructor
public class ContentImgController {

    private final ContentImgService contentImgService;
    private final ProductRepositoryV1 productRepository;

    // 이미지 업로드
    @PostMapping("/upload")
    public ResponseEntity<String> uploadContentImg(
            @RequestParam("productId") Long productId,
            @RequestParam("uploadType") UploadType uploadType,
            List<String> contentUrls
    ) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        contentImgService.uploadContentImages(product, contentUrls, uploadType);
        return ResponseEntity.status(HttpStatus.CREATED).body("이미지 업로드 완료");
    }

    // 이미지 삭제
    @DeleteMapping("/delete/{contentImgId}")
    public ResponseEntity<String> deleteContentImg(@PathVariable("contentImgId") Long contentImgId) {
        contentImgService.deleteContentImage(contentImgId);
        return ResponseEntity.status(HttpStatus.OK).body(DELETE_SUCCESS);
    }

    // 상품별 이미지 조회 (경로 리스트)
    @GetMapping("/{productId}")
    public ResponseEntity<List<String>> getProductContentImgs(@PathVariable("contentImgId") Long productId) {
        List<ContentImages> contentImages = contentImgService.getContentImages(productId);

        if (!contentImages.isEmpty()) {
            List<String> imagePaths = contentImages.stream()
                    .map(ContentImages::getImagePath)
                    .collect(Collectors.toList());
            return ResponseEntity.ok().body(imagePaths);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

















