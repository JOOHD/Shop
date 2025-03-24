package JOO.jooshop.payment.controller;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.global.Exception.PaymentCancelFailureException;
import JOO.jooshop.global.ResponseMessageConstants;
import JOO.jooshop.order.repository.OrderRepository;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.payment.entity.PaymentRefund;
import JOO.jooshop.payment.model.PaymentCancelDto;
import JOO.jooshop.payment.model.PaymentHistoryDto;
import JOO.jooshop.payment.model.PaymentRequestDto;
import JOO.jooshop.payment.repository.PaymentRepository;
import JOO.jooshop.payment.service.PaymentService;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final HttpSession httpSession;
    private final PaymentService paymentService;
    private final CartRepository cartRepository;
    private final PaymentRepository paymentRepository;
    public final OrderRepository orderRepository;
    private IamportClient iamportClient;

    @Value("${IMP_API_KEY}")
    private String apiKey;

    @Value("${imp.api.secretkey}")
    private String secretKey;

    /**
     * Iamport API 클라이언트 초기화 메서드
     * - @PostConstruct로 빈 초기화 이후 실행
     */
    @PostConstruct
    public void init() {
        this.iamportClient = new IamportClient(apiKey, secretKey);
    }

    /**
     * 결제 검증 및 결제 완료 처리
     * @param imp_uid 아임포트 결제 고유 ID
     * @param request 결제 요청 DTO (주문 정보 포함)
     * @return 아임포트 결제 응답 객체
     * @throws IamportResponseException 아임포트 통신 예외
     * @throws IOException 입출력 예외
     *
     * - POST /order/payment/{imp_uid}
     * - 결제 완료 후 아임포트로부터 결제 정보 검증 및 후처리
     */
    @PostMapping("/order/payment/{imp_uid}")
    public IamportResponse<Payment> validateIamport(@PathVariable String imp_uid, @RequestBody PaymentRequestDto request) throws IamportResponseException, IOException {
        IamportResponse<Payment> payment = iamportClient.paymentByImpUid(imp_uid);
        log.info("결제 요청 응답. 결제 내역 - 주문 번호: {}", payment.getResponse().getMerchantUid());

        // 결제 완료 후 비즈니스 로직 처리
        paymentService.processPaymentDone(payment.getResponse(), request);

        return payment;
    }

    /**
     * 주문 완료 후 세션 정보 삭제
     * - 장바구니 삭제 및 임시 주문정보 세션 제거
     *
     * - GET /order/paymentconfirm
     */
    @GetMapping("/order/paymentConfirm")
    public ResponseEntity<String> deleteSession() {
        // 세션에서 cartIds 가져오기 (결제 완료된 장바구니)
        List<Long> cartIds = (List<Long>) httpSession.getAttribute("cartIds");
        if (cartIds == null || cartIds.isEmpty()) {
            throw new NoSuchElementException("장바구니가 비어 있습니다.");
        }

        // 첫 번째 장바구니에서 사용자 ID 추출 및 검증
        Long cartMemberId = cartRepository.findById(cartIds.get(0))
                .orElseThrow(() -> new NoSuchElementException("삭제할 장바구니를 찾을 수 없습니다."))
                .getMember().getId();
        verifyUserIdMatch(cartMemberId); // 로그인 된 사용자와 요청 사용자 비교

        // 장바구니 개별 삭제
        cartIds.forEach(cartId -> {
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new NoSuchElementException("삭제할 장바구니를 찾을 수 없습니다."));
            cartRepository.delete(cart);
        });

        // 세션에서 임시 주문 정보 삭제
        httpSession.removeAttribute("temporaryOrder");
        httpSession.removeAttribute("cartIds");

        return ResponseEntity.ok("주문 및 세션 정리 완료");
    }

    /**
     * 사용자 결제 내역 조회
     * @param memberId 사용자 ID
     * @return 사용자의 결제 내역 리스트
     *
     * - GET /order/paymenthistory/{memberId}
     * - 자신의 결제 내역만 조회 가능 (서버에서 ID 검증)
     */
    @GetMapping("/paymentHistory/{memberId}")
    public ResponseEntity<List<PaymentHistoryDto>> paymentList(@PathVariable Long memberId) {
        return ResponseEntity.status(HttpStatus.OK).body(paymentService.paymentHistoryList(memberId));
    }

    /**
     * 결제 취소(환불) 요청 처리
     * @param paymentHistoryId 결제 내역 ID
     * @param requestDto 환불 요청 DTO (사유, 설명 등 포함)
     * @return 아임포트 결제 취소 응답 객체
     * @throws IamportResponseException 아임포트 통신 예외
     * @throws IOException 입출력 예외
     *
     * - POST /order/payment/cancel/{paymentHistoryId}
     * - 해당 결제 건에 대한 환불 처리 후 응답 반환
     */
    @PostMapping("/payment/cancel/{paymentHistoryId}")
    public IamportResponse<Payment> paymentCancel(@PathVariable Long paymentHistoryId, @RequestBody PaymentCancelDto requestDto) throws IamportResponseException, IOException {
        // 결제 내역 존재 여부 확인
        PaymentHistory paymentHistory = paymentRepository.findById(paymentHistoryId)
                .orElseThrow(() -> new PaymentCancelFailureException(ResponseMessageConstants.PAYMENT_CANCEL_FAILURE));

        // 환불 정보 생성
        PaymentRefund refundInfo = paymentService.getRefundInfo(paymentHistory);
        BigDecimal refundAmount = new BigDecimal(refundInfo.getAmount());

        // 아임포트 환불 요청 데이터 구성
        CancelData cancelData = new CancelData(paymentHistory.getImpUid(), true, refundAmount);
        IamportResponse<Payment> cancelResponse = iamportClient.cancelPaymentByImpUid(cancelData);

        // 환불 실패 시, 예외 처리
        if (cancelResponse.getCode() != 0) {
            throw new PaymentCancelFailureException("환불 실패 : " + cancelResponse.getMessage());
        }
        //환불 내역 정보 저장 및 처리
        paymentService.setRefundInfo(requestDto, paymentHistory, refundInfo);

        return cancelResponse;
    }
}


