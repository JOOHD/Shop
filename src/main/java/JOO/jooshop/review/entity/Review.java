package JOO.jooshop.review.entity;

import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.reviewImg.entity.ReviewImg;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review")
public class Review {
    /*@Id
    @SequenceGenerator(
            name = "review_sequence",
            sequenceName = "review_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "review_sequence"
    )*/
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    //    @PrimaryKeyJoinColumn(name = "payment")
    @OneToOne
    @JoinColumn(name = "payment_history_id")
    private PaymentHistory paymentHistory;

    @Column(name = "content", nullable = false)
    private String reviewContent;

    @Column(name = "rating", nullable = false)
    @Min(value = 1, message = "별점은 1 이상 5 이하의 정수만 가능합니다.")
    @Max(value = 5, message = "별점은 1 이상 5 이하의 정수만 가능합니다.")
    private int rating;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewImg> reviewImages = new ArrayList<>();

    @OneToMany(mappedBy = "review")
    private List<ReviewReply> replies;


    public Review(PaymentHistory paymentHistory, String reviewContent, int rating) {
        this.paymentHistory = paymentHistory;
        this.reviewContent = reviewContent;
        this.rating = rating;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateReview(String reviewContent, int rating) {
        this.reviewContent = reviewContent;
        this.rating = rating;
        this.updatedAt = LocalDateTime.now();
    }


}
