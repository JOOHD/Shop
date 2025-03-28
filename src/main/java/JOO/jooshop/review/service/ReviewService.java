package JOO.jooshop.review.service;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.payment.repository.PaymentRepository;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.review.entity.Review;
import JOO.jooshop.review.model.ReviewCreateDto;
import JOO.jooshop.review.model.ReviewDto;
import JOO.jooshop.review.repository.ReviewRepository;
import JOO.jooshop.reviewImg.ReviewImg;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import java.util.*;

import static JOO.jooshop.global.ResponseMessageConstants.*;
import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ReviewService {
    public final PaymentRepository paymentRepository;
    public final ReviewRepository reviewRepository;
    public final ProductRepositoryV1 productRepository;
    public final MemberRepositoryV1 memberRepository;
    public final ModelMapper modelMapper;
    public final ReviewImgService reviewImgService;
    public final ReviewImgRepository reviewImgRepository;

    /**
     * 리뷰 작성
     * @param request reviewTitle, reviewContent, rating
     * @param paymentId paymentHistoryId
     * @return message
     */
    public Review createReview(ReviewCreateDto request, @Nullable List<MultipartFile> images, Long paymentId) {
        PaymentHistory paymentHistory = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("해당 주문 내역을 찾을 수 없습니다."));

        // 로그인 유저 == 요청 사용자 일치 유무 확인
        verifyUserIdMatch(paymentHistory.getMember().getId());

        // 이미 Review 가 존재하는지 확인
        if (Boolean.TRUE.equals(paymentHistory.getReview())) {
            throw new IllegalStateException("이미 후기가 작성되었습니다.");
        }

        // entity 객체 생성
        Review review = new Review(paymentHistory, request.getReviewContent(), request.getRating());

        // DB 저장
        reviewRepository.save(review);

        // 결제내역에서 리부 작성 여부 true 로 변환
        paymentHistory.setReview(true);

        // 이미지파일에 이미지가 있을 경우에만
        if (images != null && !Objects.equals(images.get(0).getOriginalFilename(), "")) {
            // 이미지 업로드
            reviewImgService.uploadReviewImg(review.getReviewId(), images.stream().toList());
        }
        return review;
    }

    /**
     * 모든 리뷰 보기
     * @return
     */
    public List<ReviewDto> allReview() {
        List<Review> reviews = reviewRepository.findAll();
        if (reviews.isEmpty()) {
            return Collections.emptyList();
        }
        // new ReviewDto(review.getId(), review.getContent(), review.getRating()))
        // Review Entity -> ReviewDto 변환, (ReviewDto 의 생성자를 직접 호출하여 객체를 생성)
        return reviews.stream().map(ReviewDto::new).toList();
    }

    /**
     * 특정 상품의 리뷰 모아보기
     * @param productId
     * @return
     */
    public List<ReviewDto> findReviewByProduct(Long productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException(PRODUCT_NOT_FOUND));

        List<Review> reviews = reviewRepository.findByPaymentHistoryProduct(product);

        if (reviews.isEmpty()) {
            return null;
        }

        return reviews.stream().map(ReviewDto::new).toList();
    }

    /**
     * 특정 회원의 리뷰 모아보기
     * @param memberId
     * @return
     */
    public List<ReviewDto> findReviewByUser(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));

        List<Review> reviews = reviewRepository.findByPaymentHistoryMember(member);

        if (reviews.isEmpty()) {
            return null;
        }

        return reviews.stream().map(ReviewDto::new).toList();
    }

    /**
     * 리뷰 수정
     * @param updateRequest (reviewContent, rating)
     * @param reviewId
     * @param memberId 수정 요청 당사자
     * @return
     */
    public Review updateReview(ReviewCreateDto updateRequest, Long reviewId, Long memberId) {

        // 현재 리뷰 조회
        Review currentReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException(WRITING_NOT_FOUND));

        // 해당 리뷰 작성자의 memberId 가져옴
        Long reviewWriterId = currentReview.getPaymentHistory().getMember().getId();

        // 로그인 중인 유저의 memberId와 파라미터로 받은 memberId 가 같은지 확인
        verifyUserIdMatch(memberId);

        // 수정자가 해당 리뷰의 작성자가 아니면 접근 금지
        if (!memberId.equals(reviewWriterId)) {
            throw new SecurityException(ACCESS_DENIED);
        }

        // 리뷰 내용과 평점 업데이트
        currentReview.updateReview(updateRequest.getReviewContent(), updateRequest.getRating());

        return reviewRepository.save(currentReview);
    }

    /**
     * 리뷰 삭제
     * @param reviewId
     */
    public void deleteReview(Long reviewId, Long memberId) {

        // 현재 리뷰 조회
        Review currentReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException(WRITING_NOT_FOUND));
        // 해당 리뷰 작성자의 memberId 가져오기
        Long reviewWriterId = currentReview.getPaymentHistory().getMember().getId();

        // 리뷰에 관련된 결제 이력을 가져와 리뷰 여부를 'false'로 설정
        PaymentHistory paymentHistory = currentReview.getPaymentHistory();
        paymentHistory.setReview(false); // 리뷰 작성 여부 확인 위함

        // 로그인 중인 유저의 memberId와 파라미터로 받은 memberId가 같은지 확인
        verifyUserIdMatch(memberId);

        // 삭제 요청자가 해당 리뷰의 작성자가 아니라면 접근을 거부
        if (!memberId.equals(reviewWriterId)) {
            throw new SecurityException(ACCESS_DENIED);
        }

        // 리뷰와 관련된 모든 이미지들을 가져옴
        List<ReviewImg> reviewImgList = reviewImgRepository.findByReview_ReviewId(reviewId);

        // 이미지가 있을 경우 해당 이미지들을 삭제
        if (reviewImgList != null) {
            for (ReviewImg reviewImg : reviewImgList) {
                // 이미지 파일 삭제
                reviewImgRepository.delete(reviewImg);
                // 실제 이미지 파일 삭제
                String imagePath = "src/main/resources/static" + reviewImg.getReviewImgPath();
                ReviewImgService.deleteImageFile(imagePath);
            }
        }

        // 리뷰 삭제
        reviewRepository.delete(currentReview);
    }
}














