package JOO.jooshop.order.service;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.model.OrderDto;
import JOO.jooshop.order.repository.OrderRepository;
import JOO.jooshop.product.entity.Product;
import JOO.jooshop.product.repository.ProductRepositoryV1;
import JOO.jooshop.productManagement.entity.ProductManagement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static JOO.jooshop.global.ResponseMessageConstants.MEMBER_NOT_FOUND;
import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class OrderService {

    /*
        포트원 과정은 클라이언트에서 결제 -> 완료 -> 결제 정보를 서버로 전송 -> 서버 API 결제 완료
        1. 사용자가 장바구니에서 주문 클릭 -> 장바구니 정보 중 주문 테이블에 필요한 값을 세션에 저장 (주문 준비)
        2. 나머지 필요한 정보를 사용자에게 입력 받음
        3. (주문 완료) 결제 클릭 -> 세션+입력값 주문 테이블에 저장 -> 프론트에서 결제 API 실행
        4. 결제 완료 -> 백엔드에서 결제 완료 진행 -> 결제내역 테이블 저장
     */

    public final CartRepository cartRepository;
    public final OrderRepository orderRepository;
    public final ProductRepositoryV1 productRepository;
    public final MemberRepositoryV1 memberRepository;
    /**
     * 주문서 화면에 나타날 정보 (사용자에게 입력받지 않고 자동으로 가져와 화면에 띄워주거나 저장할 값)
     * @param cartIds card id 리스트
     * @return order 객체 반환
     */
    public Orders createOrder(List<Long> cartIds) {
        List<Cart> carts = cartRepository.findByCartIdIn(cartIds);

        // 장바구니 첫 번째 항목에서 회원 ID 가져오기
        Long memberId = carts.get(0).getMember().getId();
        verifyUserIdMatch(memberId); // 로그인 된 사용자와 요청 사용자 비교

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));

        // 주문할 상품들
        List<ProductManagement> productMgts = new ArrayList<>();
        for (Cart cart : carts) {
            ProductManagement productMgt = cart.getProductManagement();
            productMgts.add(productMgt);
        }

        // 모든 장바구니의 memberId가 동일한지 확인 (내가 로그인한 사용자니까 memberId(나) 가 선택한 장바구니가 맞는지 확인)
        boolean sameMember = carts.stream()
                .allMatch(cart -> cart.getMember().getId().equals(memberId));
        if (!sameMember || member == null) {
            // 동일하지 않거나 회원이 존재하지 않는 경우, 주문 생성 실패
            return null;
        }

        // 주문 반환
        return new Orders(member, productMgts,member.getUsername(), getProductNames(carts),calculateTotalPrice(carts),getMemberPhoneNumber(carts));
    }

    // 주문 상품 이름들을 가져오는 메서드
    private String getProductNames(List<Cart> carts) {
        StringBuilder productNamesBuilder = new StringBuilder();
        for (Cart cart : carts) {

            Long productId = cart.getProductManagement().getProduct().getProductId();
            Product product = productRepository.findById(productId).orElse(null);

            if (product != null) {
                if (!productNamesBuilder.isEmpty()) {
                    productNamesBuilder.append(", ");
                }
                productNamesBuilder.append(product.getProductName());
            }
        }
        return productNamesBuilder.toString();
    }

    // 회원 전화번호를 가져오는 메서드
    private String getMemberPhoneNumber(List<Cart> carts) {
        Long memberId = carts.get(0).getMember().getId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(MEMBER_NOT_FOUND));
        return (member != null && member.getPhone() != null) ? member.getPhone() : null;
    }

    // 총 가격을 계산하는 메서드
    private BigDecimal calculateTotalPrice(List<Cart> carts) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Cart cart : carts) {
            BigDecimal cartPrice = BigDecimal.valueOf(cart.getPrice());
            totalPrice = totalPrice.add(cartPrice);
        }
        return totalPrice;
    }

    /**
     * 주문 테이블 저장
     * @param temporaryOrder 세션에 저장된 주문서
     * @param orders 사용자에게 입력받은 주문 정보
     * @return 주문 테이블 저장
     * 
     * (createOrder) temporaryOrder = 상품 정보/총 금액 같은 자동으로 가져오는 정보만 담아둠
     * merchantUid = 사용자 직접 입력 정보(이름, 주소, 전화번호 등등)
     *
     * temporaryOrder 사용 이유 : 가격/수량을 클라이언트가 직접 수정하지 못 하도록
       ㄴ html, disable 사용하면 되는거 아님?
           ㄴ 브라우저 개발자 도구(F12)에서 disabled 속성을 제거하면 사용자가 필드를 수정할 수 있다.
     */
    public Orders orderConfirm(Orders temporaryOrder, OrderDto orders) {
        verifyUserIdMatch(temporaryOrder.getMember().getId()); // 로그인 된 사용자와 요청 사용자 비교

        String merchantUid = generationMerchantUid(); // 주문 번호 생성

        // 세션 주문서와 사용자에게 입력받은 정보 합치기
        temporaryOrder.orderConfirm(merchantUid, orders);

        return orderRepository.save(temporaryOrder);
    }

    // 주문번호 생성 메서드
    private String generationMerchantUid() {
        // 고유한 문자열 생성
        String uniqueString = UUID.randomUUID().toString().replace("-", "");
        // 현재 날짜와 시간 생성
        LocalDateTime today = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDay = today.format(formatter).replace("-", "");

        // 무작위 문자열과 현재 날짜/시간을 조합하여 주문번호 생성
        // 20250304-6f3b1333fe7049aeb3eed702f8f1cef3
        return formattedDay + '-' + uniqueString;
    }
}















