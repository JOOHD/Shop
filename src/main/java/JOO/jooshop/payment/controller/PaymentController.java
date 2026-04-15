package JOO.jooshop.payment.controller;

import JOO.jooshop.payment.model.PaymentCancelDto;
import JOO.jooshop.payment.model.PaymentHistoryDto;
import JOO.jooshop.payment.model.PaymentRequestDto;
import JOO.jooshop.payment.service.PaymentService;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/*
 * [PaymentController]
 * 기존 -> 검증 후 Redis cart:* 전체 삭제 등 일부 후처리 로직이 컨트롤러에 남아 있었음
 * 리팩토링 -> 컨트롤러는 요청 수신과 서비스 위임만 담당, 결제 후처리는 전부 서비스로 이동
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private IamportClient iamportClient;

    @Value("${IMP_API_KEY}")
    private String apiKey;

    @Value("${IMP_SECRET_KEY}")
    private String secretKey;

    @PostConstruct
    public void init() {
        this.iamportClient = new IamportClient(apiKey, secretKey);
    }

    @PostMapping("/payment/{imp_uid}")
    public ResponseEntity<IamportResponse<Payment>> validateIamport(
            @PathVariable("imp_uid") String impUid,
            @RequestBody PaymentRequestDto request
    ) throws IamportResponseException, IOException {

        IamportResponse<Payment> paymentResponse = iamportClient.paymentByImpUid(impUid);
        log.info("결제 요청 응답 - merchantUid={}", paymentResponse.getResponse().getMerchantUid());

        paymentService.processPaymentDone(paymentResponse.getResponse(), request);

        return ResponseEntity.ok(paymentResponse);
    }

    @GetMapping("/paymentHistory/{memberId}")
    public ResponseEntity<List<PaymentHistoryDto>> getPaymentHistories(@PathVariable Long memberId) {
        return ResponseEntity.ok(paymentService.getPaymentHistoriesByMemberId(memberId));
    }

    @PostMapping("/payment/cancel/{paymentHistoryId}")
    public ResponseEntity<IamportResponse<Payment>> paymentCancel(
            @PathVariable Long paymentHistoryId,
            @RequestBody PaymentCancelDto requestDto
    ) throws IamportResponseException, IOException {

        IamportResponse<Payment> cancelResponse =
                paymentService.cancelPayment(paymentHistoryId, requestDto, iamportClient);

        return ResponseEntity.ok(cancelResponse);
    }
}