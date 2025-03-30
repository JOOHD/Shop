package JOO.jooshop.reviewImg.repository;

import JOO.jooshop.reviewImg.entity.ReviewImg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImgRepository extends JpaRepository<ReviewImg, Long> {

    List<ReviewImg> findByReview_ReviewId(Long reviewId);
}
