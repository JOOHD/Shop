package JOO.jooshop.review.model;

import JOO.jooshop.review.entity.ReviewReply;
import JOO.jooshop.Inquiry.model.InquiryReplyDto;
import JOO.jooshop.review.entity.ReviewReply;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewReplyDto {
    private Long reviewReplyId;
    private Long reviewId;
    private Long replyBy;
    private String replyContent;
    private LocalDateTime createdAt;

    public ReviewReplyDto(ReviewReply reviewReply) {
        this(
                reviewReply.getReviewReplyId(),
                reviewReply.getReview().getReviewId(),
                reviewReply.getReplyBy().getId(),
                reviewReply.getReplyContent(),
                reviewReply.getCreatedAt()
        );
    }

    public static ReviewReplyDto ReplyFormRequest(ReviewReplyDto request) {
        ReviewReplyDto replyDto = new ReviewReplyDto();

        replyDto.setReplyBy(request.replyBy);
        replyDto.setReplyContent(request.replyContent);

        return replyDto;
    }
}
