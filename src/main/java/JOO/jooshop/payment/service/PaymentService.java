package JOO.jooshop.payment.service;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.global.Exception.customException.PaymentCancelFailureException;
import JOO.jooshop.global.Exception.customException.PaymentHistoryNotFoundException;
import JOO.jooshop.global.Exception.ResponseMessageConstants;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.order.entity.OrderProduct;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.repository.OrderRepository;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.payment.entity.PaymentRefund;
import JOO.jooshop.payment.entity.PaymentStatus;
import JOO.jooshop.payment.model.PaymentCancelDto;
import JOO.jooshop.payment.model.PaymentHistoryDto;
import JOO.jooshop.payment.model.PaymentRequestDto;
import JOO.jooshop.payment.repository.PaymentRefundRepository;
import JOO.jooshop.payment.repository.PaymentRepository;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PaymentService {

    /*
      ★ 결제 과정
        1. [장바구니 단계]
          - 구매자가 장바구니에 상품을 담는다. (cartIds -> Redis 저장)

        2. [결제 요청 단계]
          - 구매자 결제 버튼 클릭, client -> Iamport 결제 요청
          - Iamport 결제 성공 시, paymentService.processPaymentDone() 호출

        3. [결제 처리 단계]
          1) 구매자 로그인 상태인지 검증, DB 에서 주문/회원 정보 조회
          2) 검증 완료 시, 주문 상태 변경
          3) 임시 주문 정보 조회, Redis 에서 cartIds:{memberId} 키로 장바구니 리스트 조회
          4) OrderProduct 생성, 각 Cart -> OrderProduct 로 변환
          5) 각 OrderProduct 마다 PaymentHistory Entity 생성
          6) 아임포트 응답(impUid, 결제 수단, 전체 금액, 상품명 등)을 바탕으로 DB 저장

        4. [최종]
          - 주문(Orders), 주문상품(OrderProduct), 결제이력(PaymentHistory)이 DB에 저장됨

        ※ 주요 변경 사항
        1. Redis 저장소에 임시 주문 저장
        - 세션에서 장바구니 ID를 가져와 임시 주문 정보를 Redis에 저장,
            결제 완료 후, Redis 에서 해당 정보를 조회해 OrderProduct 를 생성
        2. OrderProduct 저장
        - 주문 완료 시, 각 상품에 대한 OrderProduct 를 생성하고 이를 DB에 저장
        3. 장바구니 처리
        - Redis 에 저장된 데이터를 기반으로 실제 OrderProduct 를 생성하여 주문을 확정

         @Qualifier("redisTemplate") 적용
         Spring Boot 의 spring-boot-starter-data-redis 의존성을 추가하면,
         Spring 이 내부적으로 아래와 같이 자동 구서을 해주기 때문에,
         내가 등록한 RedisTemplate 와 StringRedisTemplate 가 "자동 주입 중복 충돌" 된다.
         그래서 @Qualifier "여러 개의 Bean 중에서 정확히 어떤 Bean을 사용할지 선택"으로 충돌 방지
     */

    private final RedisTemplate<String, Object> redisTemplate;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final MemberRepositoryV1 memberRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;

    /**
     * 결제 완료 시, 주문 상태를 '결제 완료'로 변경하고
     * 결제 내역(PaymentHistory)을 생성하여 저장한다.
     */
    public void processPaymentDone(OrderProduct orderProduct, Payment response, PaymentRequestDto request) {
        // 로그인된 사용자가 요청한 사용자와 일치하는지 확인
        verifyUserIdMatch(request.getMemberId());

        // 주문과 회원 정보를 가져옴
        Orders order = getOrderById(request.getOrderId());
        Member member = getMemberById(request.getMemberId());

        // 주문 상태 변경 (결제 완료)
        order.updatePayStatus(PaymentStatus.COMPLETE);

        // 장바구니에서 주문된 상품 정보 처리
        List<OrderProduct> orderProducts = getOrderProductsFromRedis(orderProduct, order, member);

        // 결제 내역 생성 및 저장 (상품별)
        createPaymentHistories(response, orderProducts, order, member);
    }

    /**
     * Redis 에서 임시 주문 데이터를 가져와 OrderProduct 생성
     */
    private List<OrderProduct> getOrderProductsFromRedis(OrderProduct orderProduct, Orders order, Member member) {
        Object object = redisTemplate.opsForValue().get("cartIds:" + member.getId());
        List<Long> cartIds;

        if (object instanceof List<?>) {
            cartIds = ((List<?>) object).stream()
                    .map(o -> Long.valueOf(o.toString())) // toString 후 Long 으로 변환
                    .collect(Collectors.toList());
        } else {
            throw new IllegalStateException("Redis 에 저장된 cartIds 데이터 형식이 List 가 아닙니다.");
        }

        // 장바구니의 각 아이템을 OrderProduct 로 변환
        return cartIds.stream()
                .map(cartId -> {
                    // 장바구니 항목을 조회
                    Cart cart = cartRepository.findById(cartId)
                            .orElseThrow(() -> new NoSuchElementException("장바구니가 비어 있습니다."));

                    BigDecimal priceAtOrder = orderProduct.getPriceAtOrder(); // OrderProduct 내에서 처리된 가격
                    return orderProduct.createOrderProduct(order, cart.getProductManagement(), priceAtOrder, cart.getQuantity());
                })
                .collect(Collectors.toList());
    }

    /**
     * 사용자 결제 내역 조회
     */
    public List<PaymentHistoryDto> getPaymentHistoriesByMemberId(Long memberId) {
        List<PaymentHistory> paymentHistories = paymentRepository.findByMemberId(memberId);
        return paymentHistories.stream()
                .map(PaymentHistoryDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 결제 취소 및 환불 관련 로직
      */
    public IamportResponse<Payment> cancelPayment(Long paymentHistoryId,
                                                  PaymentCancelDto requestDto,
                                                  IamportClient iamportClient) throws IamportResponseException, IOException {
        PaymentHistory paymentHistory = paymentRepository.findById(paymentHistoryId)
                .orElseThrow(() -> new PaymentHistoryNotFoundException(ResponseMessageConstants.PAYMENT_HISTORY_NOT_FOUND));

        PaymentRefund refundInfo = createRefundInfo(paymentHistory);

        CancelData cancelData = new CancelData(paymentHistory.getImpUid(), true, new BigDecimal(refundInfo.getAmount()));
        IamportResponse<Payment> cancelResponse = iamportClient.cancelPaymentByImpUid(cancelData);

        if (cancelResponse.getCode() != 0) {
            throw new PaymentCancelFailureException("환불 실패 : " + cancelResponse.getMessage());
        }

        saveRefundInfo(requestDto, paymentHistory, refundInfo);

        return cancelResponse;
    }

    /////////// private 메서드 모음 (서비스 내부 로직 보조용) ///////////

    /**
     * 주문 ID로 주문 정보를 조회한다.
     */
    private Orders getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException(ResponseMessageConstants.ORDER_NOT_FOUND));
    }

    /**
     * 회원 ID로 회원 정보를 조회한다.
     */
    private Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(ResponseMessageConstants.MEMBER_NOT_FOUND));
    }

    /**
     * 결제 완료 후, 각 상품에 대한 결제 내역을 생성하여 저장
     */
    private void createPaymentHistories(Payment response, List<OrderProduct> orderProducts, Orders order, Member member) {
        for (OrderProduct orderProduct : orderProducts) {
            PaymentHistory paymentHistory = PaymentHistory.builder()
                    .orders(order)
                    .member(member)
                    .impUid(response.getImpUid())
                    .payMethod(response.getPayMethod())   // 결제 수단 추가
                    .totalPrice(order.getTotalPrice())    // 전체 결제 금액
                    .product(orderProduct.getProductManagement().getProduct()) // 상품 정보
                    .productName(orderProduct.getProductName()) // 상품 이름
                    .quantity(orderProduct.getQuantity()) // 상품 수량
                    .build();

            paymentRepository.save(paymentHistory);
        }
    }

    /**
     * 결제 취소 시 사용할 환불 정보(PaymentRefund)를 생성한다.
     */
    private PaymentRefund createRefundInfo(PaymentHistory paymentHistory) {
        return PaymentRefund.builder()
                .paymentHistory(paymentHistory)
                .amount(paymentHistory.getTotalPrice().intValue()) // BigDecimal을 int로 변환하여 저장
                .build();
    }

    /**
     * 결제 취소 완료 후, 환불 정보를 저장하고 결제 상태를 취소로 변경한다.
     */
    private void saveRefundInfo(PaymentCancelDto requestDto, PaymentHistory paymentHistory, PaymentRefund refundInfo) {
        paymentHistory.updateStatus(PaymentStatus.CANCELED);  // String -> Enum , getDiscription() 제거
        paymentRefundRepository.save(refundInfo);
    }

}
