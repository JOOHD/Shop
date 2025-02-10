package JOO.jooshop.review.entity;

import JOO.jooshop.members.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review_reply")
public class ReviewReply {
    @Id
    @SequenceGenerator(
            name = "review_reply_sequence",
            sequenceName = "review_reply_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "review_reply_sequence"
    )
    @Column(name = "reply_id")
    private Long reviewReplyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_by", nullable = false)
    private Member replyBy;

    @Column(name = "reply_content", nullable = false)
    private String replyContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public ReviewReply(Review review, Member replyBy, String replyContent) {
        this.review = review;
        this.replyBy = replyBy;
        this.replyContent = replyContent;
        this.createdAt = LocalDateTime.now();
    }


    public void setReview(Review review) {
        this.review = review;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
