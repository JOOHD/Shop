package JOO.jooshop.payment.service;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.global.Exception.PaymentCancelFailureException;
import JOO.jooshop.global.Exception.PaymentHistoryNotFoundException;
import JOO.jooshop.global.ResponseMessageConstants;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.entity.enums.PayMethod;
import JOO.jooshop.order.repository.OrderRepository;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.payment.entity.PaymentRefund;
import JOO.jooshop.payment.entity.PaymentStatus;
import JOO.jooshop.payment.entity.Status;
import JOO.jooshop.payment.model.PaymentCancelDto;
import JOO.jooshop.payment.model.PaymentHistoryDto;
import JOO.jooshop.payment.model.PaymentRequestDto;
import JOO.jooshop.payment.repository.PaymentRefundRepository;
import JOO.jooshop.payment.repository.PaymentRepository;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.entity.ProductManagement;
import JOO.jooshop.productManagement.repository.ProductManagementRepository;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
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

    private final ProductRepositoryV1 productRepository;
    private final OrderRepository orderRepository;
    private final MemberRepositoryV1 memberRepository;
    private final PaymentRepository paymentRepository;
    private final ProductManagementRepository productMgtRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final CartRepository cartRepository;

    /**
     * 결제 완료 시, 주문 상태를 '결제 완료'로 변경하고
     * 결제 내역(PaymentHistory)을 생성하여 저장한다.
     */
    public void processPaymentDone(Payment response, PaymentRequestDto request) {
        // 로그인된 사용자가 요청한 사용자와 일치하는지 확인
        verifyUserIdMatch(request.getMemberId());

        // 주문과 회원 정보를 가져옴
        Orders order = getOrderById(request.getOrderId());
        Member member = getMemberById(request.getMemberId());

        // 주문 상태 변경 (결제 완료)
        order.updatePayStatus(PaymentStatus.COMPLETE);

        // 결제 응답에서 필요한 정보 추출 (예시)
        String impUid = response.getImpUid();
        String payMethod = response.getPayMethod();
        int totalPrice = response.getTotalPrice();

        // 상품 ID들 (결제에 포함된 상품들)
        List<Long> inventoryIds = getInventoryIdsFromOrder(order); // order에서 상품 정보 가져오기

        // 상품 정보 (각 상품에 대한 세부 정보)
        List<Product> products = getProductsFromInventoryIds(inventoryIds); // inventoryIds를 통해 상품 정보 가져오기

        // 각 상품의 수량 정보 (여기선 주문에서의 상품 수량 정보 필요)
        List<Long> quantities = getQuantitiesFromOrder(order); // 주문에서 상품 수량 가져오기

        // 결제내역 생성 및 저장 (상품별)
        createPaymentHistories(response, inventoryIds, order, member, products, quantities, totalPrice);
    }

    /**
     * 결제 완료 후 세션에 저장된 장바구니 ID를 이용해
     * 장바구니를 삭제하고, 세션을 정리한다.
     */
    public void clearPaymentSession(HttpSession session) {
        List<Long> cartIds = (List<Long>) session.getAttribute("cartIds");
        if (cartIds == null || cartIds.isEmpty()) {
            throw new NoSuchElementException("장바구니가 비어 있습니다.");
        }

        Long memberId = cartRepository.findById(cartIds.get(0))
                .orElseThrow(() -> new NoSuchElementException("삭제할 장바구니를 찾지 못했습니다."))
                .getMember().getId();

        verifyUserIdMatch(memberId);

        cartIds.forEach(cartId -> {
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new NoSuchElementException("삭제할 장바구니를 찾을 수 없습니다."));
            cartRepository.delete(cart);
        });

        session.removeAttribute("temporaryOrder");
        session.removeAttribute("cartIds");
    }

    /**
     * 회원 ID를 기반으로 결제 이력을 조회하여 반환
     */
    public List<PaymentHistoryDto> paymentHistoryList(Long memberId) {
        verifyUserIdMatch(memberId);
        List<PaymentHistory> paymentHistories = paymentRepository.findAllByMemberId(memberId);
        return paymentHistories.stream()
                .map(PaymentHistoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 결제 추소 요청을 수행하고,
     * 성공 시, 환불 이력 (PaymentRefund)을 저장.
     */
    public IamportResponse<Payment> cancelPayment(
            Long paymentHistoryId,
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
     * 결제내역 생성 및 저장 (상품별)
     */
    private void createPaymentHistories(Payment response,
                                        List<Long> inventoryIds,
                                        Orders order,
                                        Member member,
                                        List<Product> products,
                                        List<Long> quantities,
                                        int totalPrice) {
        // inventoryIds, products, quantities를 이용하여 각각의 결제 내역을 저장
        for (int i = 0; i < inventoryIds.size(); i++) {
            Product product = products.get(i);
            Long quantity = quantities.get(i);

            PaymentHistory paymentHistory = PaymentHistory.builder()
                    .orders(order)
                    .member(member)
                    .impUid(response.getImpUid())
                    .payMethod(response.getPayMethod())   // 결제 수단 추가
                    .totalPrice(totalPrice)               // 전체 결제 금액
                    .product(product)                     // 상품 정보
                    .productName(product.getProductName()) // 상품 이름
                    .quantity(quantity)                   // 상품 수량
                    .build();

            paymentRepository.save(paymentHistory);
        }
    }
    /**
     * 주문에서 상품 ID들 가져오기
     */
    private List<Long> getInventoryIdsFromOrder(Orders order) {
        // 주문에서 상품 ID를 가져오는 로직을 작성
        // 예시: order.getItems() -> inventoryId를 포함한 List
        return order.getItems().stream()
                .map(item -> item.getProduct().getId()) // 각 상품의 ID를 추출
                .collect(Collectors.toList());
    }

    /**
     * 주문에서 상품 정보 가져오기 (상품 ID로)
     */
    private List<Product> getProductsFromInventoryIds(List<Long> inventoryIds) {
        return inventoryIds.stream()
                .map(id -> productRepository.findById(id).orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다.")))
                .collect(Collectors.toList());
    }

    /**
     * 주문에서 상품 수량 정보 가져오기
     */
    private List<Long> getQuantitiesFromOrder(Orders order) {
        return order.getItems().stream()
                .map(item -> item.getQuantity())  // 주문된 수량
                .collect(Collectors.toList());
    }

    /**
     * 결제 취소 시 사용할 환불 정보(PaymentRefund)를 생성한다.
     */
    private PaymentRefund createRefundInfo(PaymentHistory paymentHistory) {
        return PaymentRefund.builder()
                .paymentHistory(paymentHistory)
                .amount(paymentHistory.getTotalPrice()) // "결제된 총 금액"
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
