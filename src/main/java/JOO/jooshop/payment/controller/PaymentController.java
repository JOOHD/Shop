package JOO.jooshop.payment.controller;

import JOO.jooshop.order.entity.OrderProduct;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.payment.model.PaymentCancelDto;
import JOO.jooshop.payment.model.PaymentHistoryDto;
import JOO.jooshop.payment.model.PaymentRequestDto;
import JOO.jooshop.payment.repository.PaymentRepository;
import JOO.jooshop.payment.service.PaymentService;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    /** 25.04.26, 2차 리팩토링
     *  Controller 는 "요청 받고, 서비스에 위임만" -> 비즈니스 로직은 Service
     *  예외처리 문구 통일, 세션 처리 로직 Service 분리
     *  String 값이였던 상태 값은, Enum 으로 따로 관리 추천 (PaymentStatus)
     * 
     *  25.04.27 3차 리팩토링
     *  1. Redis 사용
     *     - RedisTemplate 을 이용해 Redis 에 데이터 저장, 삭제 로직 추가
     *     - order:products 라는 키로 주문한 상품 정보를 Redis 에 저장 (매번 주문 때 마다)
     *     - 결제 완료 후, Redis 에 저장된 장바구니 정보 삭제 로직 구현 (validateIamport)
     *  2. OrderProduct 엔티티 연결
     *     - 주문 확정 후 생성되는 상품 정보로, Redis 에 저장되는 상품 데이터 관리
     *  3. processPaymentDone service method
     *     - 결제가 완료된 후 해당 결제 내역을 처리하는 비즈니스 로직을
     *          paymentService.processPaymentDone 메서드로 위임
     *     - 이 메서드는 주문 상태 변경 및 주문한 상품에 대한 처리를 담당
     *  4. 세션 삭제
     *     - 결제 완료 후 세션에서 임시 주문 정보(temporaryOrder & cartIds)를 삭제 기능 추가.
     */
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HttpSession httpSession;
    private IamportClient iamportClient;

    @Value("${IMP_API_KEY}")
    private String apiKey;

    @Value("${IMP_SECRET_KEY}")
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
     * - 결제 완료 후 아임포트로부터 결제 정보 검증 및 후처리
     */
    @PostMapping("/order/payment/{imp_uid}")
    public ResponseEntity<IamportResponse<Payment>> validateIamport(
            @PathVariable("imp_uid") String imp_uid,
            @RequestBody PaymentRequestDto request,
            OrderProduct orderProduct) throws IamportResponseException, IOException {

        // 결제 정보 검증
        IamportResponse<Payment> paymentResponse = iamportClient.paymentByImpUid(imp_uid);
        log.info("결제 요청 응답. 결제 내역 - 주문 번호: {}", paymentResponse.getResponse().getMerchantUid());

        // 결제 완료 후 비즈니스 로직 처리
        paymentService.processPaymentDone(orderProduct, paymentResponse.getResponse(), request);

        // Redis 에서 장바구니 정보 삭제 (결제 완료 후)
        Set<String> cartKeys = redisTemplate.keys("cart:*");
        if (cartKeys != null) {
            redisTemplate.delete(cartKeys); // Redis 에서 삭제, httpSessiion.remove.. 필요 x
        }

        return ResponseEntity.ok(paymentResponse);
    }

    /**
     * 사용자 결제 내역 조회
     * - 자신의 결제 내역만 조회 가능 (서버에서 ID 검증)
     */
    @GetMapping("/paymentHistory/{memberId}")
    public List<PaymentHistoryDto> createPaymentHistories(Long memberId) {
        List<PaymentHistory> paymentHistories = paymentRepository.findByMemberId(memberId);
        return paymentHistories.stream()
                .map(paymentHistory -> new PaymentHistoryDto(paymentHistory)) // PaymentHistory를 PaymentHistoryDto로 변환
                .collect(Collectors.toList());
    }

    /**
     * 결제 취소(환불) 요청 처리
     * - 해당 결제 건에 대한 환불 처리 후 응답 반환
     */
    @PostMapping("/payment/cancel/{paymentHistoryId}")
    public ResponseEntity<IamportResponse<Payment>> paymentCancel(
            @PathVariable("paymentHistoryId") Long paymentHistoryId,
            @RequestBody PaymentCancelDto requestDto) throws IamportResponseException, IOException {

        IamportResponse<Payment> cancelResponse = paymentService.cancelPayment(paymentHistoryId, requestDto, iamportClient);
        return ResponseEntity.ok(cancelResponse);
    }

}


