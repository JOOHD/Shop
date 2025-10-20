package JOO.jooshop.thumbnail.controller;

import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.thumbnail.service.ThumbnailServiceV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;

import static JOO.jooshop.global.Exception.ResponseMessageConstants.DELETE_SUCCESS;
import static JOO.jooshop.global.Exception.ResponseMessageConstants.PRODUCT_NOT_FOUND;

@Slf4j
@RestController
@RequestMapping("/api/v1/thumbnail")
@RequiredArgsConstructor
public class ThumbnailApiControllerV1 {

    private final ThumbnailServiceV1 thumbnailService;
    private final ProductRepositoryV1 productRepository;

    // 썸네일 업로드
    @PostMapping("/upload")
    public ResponseEntity<String> uploadThumbnail(@RequestParam("productId") Long productId, @RequestParam("image") List<MultipartFile> images) {
        Product product = productRepository.findByProductId(productId).orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));
        thumbnailService.uploadThumbnail(product, images);
        return ResponseEntity.status(HttpStatus.CREATED).body("썸네일 업로드 완료");
    }

    // 썸네일 삭제
    @DeleteMapping("/delete/{thumbnailId}")
    public ResponseEntity<String> deleteThumbnail(@PathVariable("thumbnailId") Long thumbnailId) {
        thumbnailService.deleteThumbnail(thumbnailId);
        return ResponseEntity.status(HttpStatus.OK).body(DELETE_SUCCESS);
    }

    // 상품 id로 썸네일 조회 (경로 리스트)
    @GetMapping("/{productId}")
    public ResponseEntity<List<String>> getProductThumbnails(@PathVariable("productId") Long productId) {
        List<String> thumbnails = thumbnailService.getProductThumbnails(productId);
        if (!thumbnails.isEmpty()) {
            return ResponseEntity.ok().body(thumbnails);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
