package JOO.jooshop.review.service;

import JOO.jooshop.global.authorization.RequiresRole;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.review.entity.Review;
import JOO.jooshop.review.entity.ReviewReply;
import JOO.jooshop.review.model.ReviewReplyDto;
import JOO.jooshop.review.repository.ReviewReplyRepository;
import JOO.jooshop.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

import static JOO.jooshop.global.exception.ResponseMessageConstants.*;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ReviewReplyService {
    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewRepository reviewRepository;
    private final MemberRepositoryV1 memberRepository;

    /**
     * 리뷰 댓글 작성
     * @param replyDto
     * @param reviewId
     * @return
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public Long createReply(ReviewReplyDto  replyDto, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("해당 리뷰를 찾을 수 없습니다. reviewId: " + reviewId));

        Member member = memberRepository.findById(replyDto.getReplyBy())
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));

        // reviewReply = ReviewReply(review=Review(id=1, content="좋은 상품입니다."), member=Member(id=2, name="John Doe"), replyContent="상품이 정말 좋습니다!")
        ReviewReply reviewReply = new ReviewReply(review, member, replyDto.getReplyContent());

        reviewReplyRepository.save(reviewReply);

        return reviewReply.getReviewReplyId();
    }

    /**
     * 리뷰 댓글 삭제
     * @param replyId
     */
    @RequiresRole({MemberRole.ADMIN, MemberRole.SELLER})
    public void deleteReply(Long replyId) {
        ReviewReply currentReply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new NoSuchElementException(WRITING_NOT_FOUND));
        reviewReplyRepository.delete(currentReply);
    }
}
