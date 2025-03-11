package JOO.jooshop.payment.controller;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.global.Exception.PaymentCancelFailureException;
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

    @PostConstruct
    public void init() {
        this.iamportClient = new IamportClient(apiKey, secretKey);
    }

    @PostMapping("/order/payment/{imp_uid}")
    public IamportResponse<Payment> validateIamport(@PathVariable String imp_uid, @RequestBody PaymentRequestDto request) throws IamportResponseException, IOException {
        IamportResponse<Payment> payment = iamportClient.paymentByImpUid(imp_uid);
        log.info("결제 요청 응답. 결제 내역 - 주문 번호: {}", payment.getResponse().getMerchantUid());

        paymentService.processPaymentDone(payment.getResponse(), request);

        return payment;
    }

    @GetMapping("/order/paymentconfirm")
    public void deleteSession() {
        List<Long> cartIds = (List<Long>) httpSession.getAttribute("cartIds");
        if (cartIds == null || cartIds.isEmpty()) {
            throw new NoSuchElementException("장바구니가 비어 있습니다.");
        }

        Long cartMemberId = cartRepository.findById(cartIds.get(0))
                .orElseThrow(() -> new NoSuchElementException("삭제할 장바구니를 찾을 수 없습니다."))
                .getMember().getId();
        verifyUserIdMatch(cartMemberId); // 로그인 된 사용자와 요청 사용자 비교

        cartIds.forEach(cartId -> {
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new NoSuchElementException("삭제할 장바구니를 찾을 수 없습니다."));
            cartRepository.delete(cart);
        });

        // 세션에서 임시 주문 정보 삭제
        httpSession.removeAttribute("temporaryOrder");
        httpSession.removeAttribute("cartIds");
    }

    @GetMapping("/paymenthistory/{memberId}")
    public ResponseEntity<List<PaymentHistoryDto>> paymentList(@PathVariable Long memberId) {
        return ResponseEntity.status(HttpStatus.OK).body(paymentService.paymentHistoryList(memberId));
    }

    @PostMapping("/payment/cancel/{paymentHistoryId}")
    public IamportResponse<Payment> paymentCancel(@PathVariable Long paymentHistoryId, @RequestBody PaymentCancelDto requestDto) throws IamportResponseException, IOException {
        PaymentHistory paymentHistory = paymentRepository.findById(paymentHistoryId)
                .orElseThrow(() -> new NoSuchElementException("해당 결제 내역을 찾을 수 없습니다."));

        PaymentRefund refundInfo = paymentService.getRefundInfo(paymentHistory);
        BigDecimal refundAmount = new BigDecimal(refundInfo.getAmount());

        CancelData cancelData = new CancelData(paymentHistory.getImpUid(), true, refundAmount);
        IamportResponse<Payment> cancelResponse = iamportClient.cancelPaymentByImpUid(cancelData);

        if (cancelResponse.getCode() != 0) {
            throw new PaymentCancelFailureException("환불 실패 : " + cancelResponse.getMessage());
        }

        paymentService.setRefundInfo(requestDto, paymentHistory, refundInfo);

        return cancelResponse;
    }
}


