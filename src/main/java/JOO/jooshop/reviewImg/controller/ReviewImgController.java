package JOO.jooshop.reviewImg.controller;

import JOO.jooshop.reviewImg.entity.ReviewImg;
import JOO.jooshop.reviewImg.service.ReviewImgService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/review/img")
@RequiredArgsConstructor
public class ReviewImgController {
    private final ReviewImgService reviewImgService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadReviewImg(@RequestParam("reviewId") Long reviewId, @RequestParam("image") List<MultipartFile> images) {
        reviewImgService.uploadReviewImg(reviewId, images);
        return ResponseEntity.status(HttpStatus.CREATED).body("사진 업로드 완료");
    }

    @DeleteMapping("/delete/{reviewImgId}")
    public ResponseEntity<String> deleteReviewImg(@PathVariable Long reviewImgId) {
        reviewImgService.deleteReviewImg(reviewImgId);
        return ResponseEntity.status(HttpStatus.OK).body("사진 삭제 완료");
    }

    @GetMapping("/{reviewId")
    public ResponseEntity<List<String>> getReviewImgs(@PathVariable Long reviewId) {
        List<ReviewImg> images = reviewImgService.getReviewImg(reviewId);
        if (!images.isEmpty()) {
            List<String> imagePaths = images.stream()
                    .map(ReviewImg::getReviewImgPath)
                    .collect(Collectors.toList());
            return ResponseEntity.ok().body(imagePaths);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
