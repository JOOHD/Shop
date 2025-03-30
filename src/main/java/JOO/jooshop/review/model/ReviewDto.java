package JOO.jooshop.review.model;

import JOO.jooshop.review.entity.Review;
import JOO.jooshop.reviewImg.entity.ReviewImg;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDto {
    private Long reviewId;
    private Long paymentHistoryId;
    private String reviewContent;
    @Min(value = 1, message = "별점은 1 이상 5 이하의 정수만 가능합니다.")
    @Max(value = 5, message = "별점은 1 이상 5 이하의 정수만 가능합니다.")
    private int rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long memberId;
    private String writer;
    private Long productId;
    private String productName;
    private List<String> reviewImgPaths;
    private List<ReviewReplyDto> replies;

    public ReviewDto(Review review) {
        this(
                review.getReviewId(),
                review.getPaymentHistory().getId(),
                review.getReviewContent(),
                review.getRating(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getPaymentHistory().getOrders().getMember().getId(),
                review.getPaymentHistory().getOrders().getMember().getUsername(),
                review.getPaymentHistory().getProduct().getProductId(),
                review.getPaymentHistory().getProduct().getProductName(),
                review.getReviewImages().stream().map(ReviewImg::getReviewImgPath).toList(),

                review.getReplies() != null ? review.getReplies().stream().map(ReviewReplyDto::new).toList() : null
        );
    }

}
