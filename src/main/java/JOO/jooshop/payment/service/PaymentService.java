package JOO.jooshop.payment.service;

import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.global.Exception.PaymentHistoryNotFoundException;
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
import java.util.Optional;
import java.util.stream.Collectors;

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
        List<PaymentHistory> paymentHistories = getFindByMemberId(memberId);

        /*
        List<PaymentHistoryDto> paymentHistoryDtos = new ArrayList<>();
        for (PaymentHistory paymentHistory : paymentHistories) {
            PaymentHistoryDto paymentHistoryDto = new PaymentHistoryDto(paymentHistory);
            paymentHistoryDtos.add(paymentHistoryDto);
        }*/

        List<PaymentHistoryDto> paymentHistoryDtos = paymentHistories.stream()
                .map(PaymentHistoryDto::new)   // PaymentHistory -> PaymentHistoryDto 변환
                .collect(Collectors.toList()); // 리스트로 반환
        
        // 결제내역 응답 반환
        return paymentHistoryDtos;
    }

    private List<PaymentHistory> getFindByMemberId(Long memberId) {
        return Optional.ofNullable(paymentRepository.findByMemberId(memberId))
                .filter(paymentHistories -> !paymentHistories.isEmpty())
                .orElseThrow(() -> new PaymentHistoryNotFoundException(ResponseMessageConstants.PAYMENT_HISTORY_NOT_FOUND_BY_MEMBER));
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
     * @param request 클라이언트에서 전송한 환불 요청 정보
     * @param paymentHistory 환불하려는 결제 내역
     * @param paymentInfo 환불 전 검증 후 얻은 환불 정보(PG사 UID, 금액 등)
     * @return 저장된 환불 정보
     *
     * 메서드 흐름
     * [Client] -> 환불 요청 (DTO) -> [PaymentService.setRefundInfo()]
     *   -> paymentHistory 조회 (verifyUserIdMatch)
     *   -> 부분/전체 환불 계산
     *   -> afterChecksum 계산
     *   -> PaymentRefund 생성
     *      -> paymentHistory, impUid, refundAmount, etc...
     *   -> paymentRefundRepository.save(refund)
     *   -> paymentHistory 상태 변경 및 checksum 동기화
     *   -> 아임포트 API로 실제 환불 처리
     */
    public PaymentRefund setRefundInfo(PaymentCancelDto request, PaymentHistory paymentHistory, PaymentRefund paymentInfo) {

        // 결제 고유번호(아임포트 UID), 환불 금액, 현재 checksum 조회
        String impUid = paymentInfo.getImpUid(); // impUid 는 String 문자열 형식이다.
        Integer amount = paymentInfo.getAmount();
        Integer checksum = paymentInfo.getChecksum();

        // 같은 impUid 를 가진 결제 내역들을 전부 조회(여러 주문이 하나의 결제로 묶여있을 수 있다)
        List<PaymentHistory> paymentHistoriesWithSameUid = getPaymentHistoriesWithSameUid(impUid);

        // 환불 후 남은 금액 계산 (기존 checksum - 환불금액)
        Integer afterChecksum = checksum - amount;

        // 같은 impUid를 가진 모든 결제 내역에 대해 남은 금액 업데이트
        for (PaymentHistory history : paymentHistoriesWithSameUid) {
            history.setTotalPrice(afterChecksum);
        }

        // 현재 환불 대상 결제 상태를 'CANCELED'로 변경
        paymentHistory.setStatusType(Status.CANCELED);

        // 환불 정보를 생성 (vbank일 경우와 일반 결제일 경우를 구분하여 생성)
        PaymentRefund paymentRefund = buildRefund(paymentHistory, request, paymentInfo);

        // 생성된 환불 내역을 DB에 저장
        paymentRefundRepository.save(paymentRefund);

        // 저장된 환불 내역 반환
        return paymentRefund;
    }
    private List<PaymentHistory> getPaymentHistoriesWithSameUid(String impUid) {
        return Optional.ofNullable(paymentRepository.findByImpUid(impUid))
                .filter(paymentHistory -> !paymentHistory.isEmpty())
                .orElseThrow(() -> new PaymentHistoryNotFoundException(ResponseMessageConstants.PAYMENT_HISTORY_NOT_FOUND_BY_IMPUID));
    }

    /**
     * 환불 정보를 생성하는 메서드
     * - 가상계좌(vbank) 결제일 경우 환불 계좌 정보 포함
     * - 일반 결제일 경우 환불 사유만 포함
     *
     * @param paymentHistory 환불할 결제 내역
     * @param request        클라이언트로부터 전달받은 결제 취소 요청 정보(DTO)
     * @param paymentInfo    기존 결제 정보(impUid, 환불금액, checksum 등 포함)
     * @return 생성된 환불 내역 엔티티 반환
     */
    private PaymentRefund buildRefund(PaymentHistory paymentHistory, PaymentCancelDto request, PaymentRefund paymentInfo) {

        // 가상계좌 환불일 경우 추가적인 정보(환불 계좌 정보 등)를 포함하여 PaymentRefund 생성
        if (request.getPayMethod() == PayMethod.vbank) {
            return new PaymentRefund(
                    paymentHistory,                        // 환불할 결제 내역 엔티티
                    paymentInfo.getImpUid(),               // 아임포트 고유 결제번호
                    paymentInfo.getAmount(),               // 환불 금액
                    paymentHistory.getOrders().getPhoneNumber(), // 주문자의 연락처
                    paymentInfo.getChecksum(),             // 환불 후 남은 금액 (checksum)
                    request.getReason(),                   // 환불 사유
                    request.getRefundHolder(),             // 환불 받을 계좌 예금주
                    request.getRefundBank(),               // 환불 받을 은행명
                    request.getRefundAccount()             // 환불 받을 계좌번호
            );
        }

        // 일반 결제 환불일 경우 기본 환불 정보만 포함하여 PaymentRefund 생성
        return new PaymentRefund(
                paymentHistory,                        // 환불할 결제 내역 엔티티
                paymentInfo.getImpUid(),               // 아임포트 고유 결제번호
                paymentInfo.getAmount(),               // 환불 금액
                paymentHistory.getOrders().getPhoneNumber(), // 주문자의 연락처
                paymentInfo.getChecksum(),             // 환불 후 남은 금액 (checksum)
                request.getReason()                    // 환불 사유
        );
    }
}
