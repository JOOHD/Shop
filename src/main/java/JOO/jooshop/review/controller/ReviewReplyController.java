package JOO.jooshop.review.controller;

import JOO.jooshop.review.model.ReviewReplyDto;
import JOO.jooshop.review.service.ReviewReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static JOO.jooshop.global.Exception.ResponseMessageConstants.*;

@RestController
@RequestMapping("/api/v1/review/reply")
@RequiredArgsConstructor
public class ReviewReplyController {
    private final ReviewReplyService replyService;

    @PostMapping("/new/{reviewId}")
    public ResponseEntity<String> createReply(@Valid @RequestBody ReviewReplyDto request, @PathVariable("reviewId") Long reviewId) {
        ReviewReplyDto replyDto = ReviewReplyDto.ReplyFormRequest(request);
        Long createdId = replyService.createReply(replyDto, reviewId);

        return ResponseEntity.status(HttpStatus.CREATED).body("리뷰 답변 완료");
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReply(@PathVariable("replyId") Long replyId) {
        replyService.deleteReply(replyId);
        return ResponseEntity.ok().body(DELETE_SUCCESS);
    }
}
