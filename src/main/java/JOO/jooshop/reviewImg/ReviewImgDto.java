package JOO.jooshop.reviewImg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "reviewImgId")
@AllArgsConstructor
public class ReviewImgDto {
    private Long reviewImgId;
    private String imageUrl;
    private Long reviewId;

    public ReviewImgDto(ReviewImg reviewImg) {
        this(
                reviewImg.getReviewImgId(),
                reviewImg.getReviewImgPath(),
                reviewImg.getReview().getReviewId()
        );
    }
}
