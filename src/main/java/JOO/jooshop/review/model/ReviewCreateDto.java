package JOO.jooshop.review.model;

import JOO.jooshop.review.entity.Review;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewCreateDto {
    private String reviewContent;
    @Min(value = 1, message = "별점은 1 이상 5 이하의 정수만 가능합니다.")
    @Max(value = 5, message = "별점은 1 이상 5 이하의 정수만 가능합니다.")
    private int rating;

    public ReviewCreateDto(Review review) {
        this(
                review.getReviewContent(),
                review.getRating()
        );
    }
}
