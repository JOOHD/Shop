package JOO.jooshop.payment.service;

import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.global.ResponseMessageConstants;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.entity.enums.PayMethod;
import JOO.jooshop.order.repository.OrderRepository;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.payment.entity.PaymentRefund;
import JOO.jooshop.payment.entity.Status;
import JOO.jooshop.payment.model.PaymentCancelDto;
import JOO.jooshop.payment.model.PaymentHistoryDto;
import JOO.jooshop.payment.model.PaymentRequestDto;
import JOO.jooshop.payment.repository.PaymentRefundRepository;
import JOO.jooshop.payment.repository.PaymentRepository;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.repository.ProductManagementRepository;
import com.siot.IamportRestClient.response.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PaymentService {

    /*
        사용자 철수 → 장바구니 상품 선택 → 결제 요청 → Iamport 응답 수신
           ↓
        PaymentService.processPaymentDone()
           ↓
        주문 상태 변경 → 결제 내역 생성 → 결제 내역 저장
           ↓
        철수는 내 결제 내역 조회 가능!
     */

    private final OrderRepository orderRepository;
    private final MemberRepositoryV1 memberRepository;
    private final PaymentRepository paymentRepository;
    private final ProductManagementRepository productMgtRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final CartRepository cartRepository;

    private static final String PAYMENT_STATUS_COMPLETE = "완료";
    private static final String PAYMENT_STATUS_CANCELED = "취소";

    /**
     * 결제 완료 후 처리(주문 상태를 업데이트하고, 결제 내역을 생성하여 DB에 저장)
     * @param response 아임포트 결제 완료 응답 객체 (결제 관련 정보가 포함)
     * @param request 결제 요청 DTO (주문 ID, 회원 ID, 결제 금액 등의 요청 데이터 포함)
     */
    public void processPaymentDone(Payment response, PaymentRequestDto request) {

        Long orderId = request.getOrderId();
        Long memberId = request.getMemberId();

        verifyUserIdMatch(memberId); // 로그인 된 사용장와 요청 사용자 비교

        Orders order = getOrderById(orderId);
        updatePaymentStatus(order);

        Member member = getMemberById(memberId);

        // 주문한 상품들에 대해 각각 결제내역 저장
        createPaymentHistory(response, request.getInventoryIdList(), order, member, request.getPrice().intValue());
    }
    private Orders getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException(ResponseMessageConstants.ORDER_NOT_FOUND));
    }

    private Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(ResponseMessageConstants.MEMBER_NOT_FOUND));
    }

    private void updatePaymentStatus(Orders order) {
        order.setPaymentStatus(true); // 결제 완료 상태로 업데이트
    }

    /**
     * createPaymentHistory : 주문한 상품들에 대해 각각 결제 내역을 생성하고 DB에 저장
     * ProductManagement : 결제내역에서 쓰임 -> 재고 차감을 위함
     * @param response         아임포트 결제 완료 응답 객체
     * @param productMgtIdList 주문한 상품의 상품관리 ID 리스트
     * @param order            주문 엔티티 (주문정보)
     * @param member           결제한 회원 엔티티 (회원정보)
     * @param totalPrice       결제 총 금액
     */
    private void createPaymentHistory(Payment response, List<Long> productMgtIdList, Orders order, Member member, Integer totalPrice) {
        for (Long productMgtId : productMgtIdList) {
            // 결제 응답으로부터 필요한 결제 정보를 추출한다.
            String impUid = response.getImpUid();         // 아임포트에서 발급한 고유 결제번호
            String payMethod = response.getPayMethod();   // 결제 수단 (카드, 가상계좌 등)
            BigDecimal payAmount = response.getAmount();  // 결제된 금액 (총 결제 금액)
            String bankCode = response.getBankCode();     // 은행 코드 (가상계좌일 경우 값 존재, 카드일 경우 null)
            String bankName = response.getBankName();     // 은행명 (가상계좌일 경우 값 존재, 카드일 경우 null)
            String buyerAddr = response.getBuyerAddr();   // 구매자 주소
            String buyerEmail = response.getBuyerEmail(); // 구매자 이메일

            ProductManagement productMgt = productMgtRepository.findById(productMgtId)
                    .orElseThrow(() -> new NoSuchElementException(ResponseMessageConstants.PRODUCT_NOT_FOUND));

            // 해당 상품관리 정보를 기준으로 장바구니에서 수량을 조회한다. (주문할 때 장바구니 기준으로 수량을 가져옴)
            Long quantity = cartRepository.findByProductManagement(productMgt).getQuantity();

            // 상품관리에서 실제 상품 엔티티를 가져온다.(option/stock 이 어느 상품에 소속인지 알기 위함)
            //      -> 결제내역에서는 상품명, 옵션명, 수량, 가격, 썸네일 등을 종합적으로 보여줘야
            Product product = productMgt.getProduct();

            // 상품 옵션(색상, 사이즈)을 문자열로 만든다. (예: 블랙, M)
            String option = productMgt.getColor().getColor() + ", " + productMgt.getSize().toString(); // 상품옵션 문자열로 저장

            // 결제 내역 엔티티를 생성 = paymentHistory
            PaymentHistory paymentHistory = new PaymentHistory(
                    impUid,                      // 아임포트 결제 고유 번호
                    member,                      // 결제한 회원 정보
                    order,                       // 주문 정보
                    product,                     // 상품 정보
                    product.getProductName(),    // 상품 이름
                    option,                      // 상품 옵션 (색상, 사이즈)
                    quantity,                    // 구매 수량
                    product.getPrice(),          // 상품 개당 가격
                    payAmount.intValue(),        // 결제된 총 금액
                    Status.COMPLETE_PAYMENT,     // 결제 상태 (완료)
                    payMethod,                   // 결제 수단
                    bankCode,                    // 은행 코드 (가상계좌 전용)
                    bankName,                    // 은행명
                    buyerAddr,                   // 구매자 주소
                    buyerEmail                   // 구매자 이메일
            );

            // 생성한 결제 내역을 DB에 저장한다.
            paymentRepository.save(paymentHistory);
        }
    }

    /**
     * paymentHistoryList : 내 결제내역 모아보기
     * @param memberId 사용자 ID
     * @return 결제내역 DTO 리스트
     */
    public List<PaymentHistoryDto> paymentHistoryList(Long memberId) {

        // 로그인된 사용자와 요청 사용자가 일치하는지 검증
        verifyUserIdMatch(memberId);

        // 해당 사용자의 결제 내역을 DB 에서 전부 조회
        List<PaymentHistory> paymentHistories = paymentRepository.findByMemberId(memberId);

        // 클라이언트에 응답할 DTO 리스트 초기화
        List<PaymentHistoryDto> paymentHistoryDtos = new ArrayList<>();

        // 결제내역 하나씩 DTO, 변환 후 리스트에 추가
        for (PaymentHistory paymentHistory : paymentHistories) {
            PaymentHistoryDto paymentHistoryDto = new PaymentHistoryDto(paymentHistory);
            paymentHistoryDtos.add(paymentHistoryDto);
        }
        // 결제내역 응답 반환
        return paymentHistoryDtos;
    }

    /**
     * getRefundInfo : 결제 취소 정보 검증
     * (환불 전에 검증하고 환불에 필요한 정보 추출)
     * @param paymentHistory 결제 내역 엔티티
     * @return 환불에 필요한 정보가 담긴 PaymentRefund 객체
     */
    public PaymentRefund getRefundInfo(PaymentHistory paymentHistory) {
        // PG사와 연동되는 결제 식별값
        String impUid = paymentHistory.getImpUid();
        // 현재 남아있는 전체 결제 금액 (환불 가능 금액의 기준)
        Integer beforeChecksum = paymentHistory.getTotalPrice();
        // 이번에 환불하고자 하는 금액 (상품 단위일 수도 있음)
        Integer refundAmount = paymentHistory.getPrice();
        // 전액 환불이 이미 끝난 경우 예외 발생
        if (beforeChecksum == 0) {
            throw new IllegalArgumentException("이미 전액 환불 완료된 주문건입니다.");
        }
        // 환불 요청에 필요한 정보를 묶어서 반환
        return new PaymentRefund(impUid, refundAmount, beforeChecksum);
    }

    /**
     * setRefundInfo : 결제 취소 진행 후 정보 저장
     * (실제 환불 처리를 수행하고 DB에 상태 반영)
     * @param requestDto 클라이언트에서 전송한 환불 요청 정보
     * @param paymentHistory 환불하려는 결제 내역
     * @param paymentInfo 환불 전 검증 후 얻은 환불 정보(PG사 UID, 금액 등)
     * @return 저장된 환불 정보
     */
    public PaymentRefund setRefundInfo(PaymentCancelDto request, PaymentHistory paymentHistory, PaymentRefund paymentInfo) {
        String impUid = paymentInfo.getImpUid();
        Integer amount = paymentInfo.getAmount();
        Integer checksum = paymentInfo.getChecksum();

        List<PaymentHistory> paymentHistoriesWithSameUid = paymentRepository.findByImpUid(impUid);

        Integer afterChecksum = checksum - amount;

        for (PaymentHistory history : paymentHistoriesWithSameUid) {
            history.setTotalPrice(afterChecksum);
        }

        paymentHistory.setStatusType(Status.CANCELED);

        PaymentRefund paymentRefund = buildRefund(paymentHistory, request, paymentInfo);

        paymentRefundRepository.save(paymentRefund);

        return paymentRefund;
    }

    private PaymentRefund buildRefund(PaymentHistory paymentHistory, PaymentCancelDto request, PaymentRefund paymentInfo) {
        if (request.getPayMethod() == PayMethod.vbank) {
            return new PaymentRefund( // buildRefund 메서드의 환불 처리 정보
                    paymentHistory,
                    paymentInfo.getImpUid(),
                    paymentInfo.getAmount(),
                    paymentHistory.getOrders().getPhoneNumber(),
                    paymentInfo.getChecksum(),
                    request.getReason(),
                    request.getRefundHolder(),
                    request.getRefundBank(),
                    request.getRefundAccount()
            );
        }
        return new PaymentRefund( // setRefundInfo 메서드의 환불 처리 정보
                paymentHistory,
                paymentInfo.getImpUid(),
                paymentInfo.getAmount(),
                paymentHistory.getOrders().getPhoneNumber(),
                paymentInfo.getChecksum(),
                request.getReason()
        );
    }
}
