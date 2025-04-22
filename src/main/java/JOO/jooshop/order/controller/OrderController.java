package JOO.jooshop.order.controller;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.order.entity.Orders;
import JOO.jooshop.order.entity.TemporaryOrderRedis;
import JOO.jooshop.order.model.OrderDto;
import JOO.jooshop.order.model.OrderResponseDto;
import JOO.jooshop.order.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static JOO.jooshop.global.ResponseMessageConstants.MEMBER_NOT_FOUND;
import static JOO.jooshop.global.authorization.MemberAuthorizationUtil.verifyUserIdMatch;


@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final HttpSession httpSession;
    private final ModelMapper modelMapper;
    
    /*
        1. 사용자가 장바구니에서 주문을 클릭,
        2. 해당 장바구니들의 id를 리스트로 받는다.
        3. 장바구니에서 상품 정보 등 필요한 정보를 찾아 주문 테이블을 생성, 이를 세션에 임시 저장

        사용자가 이름, 주소 등 주문 정보를 입력 후 결제하기를 누르면 주문 테이블이 먼저 저장된다.
     */

    /**
     * 주문서에 나타낼 정보
     * @return
     */
    @PostMapping("/create")
    public ResponseEntity<String> createOrder(@RequestBody OrderDto dto) {
        List<Long> cartIds = dto.getCartIds();
        if (cartIds.isEmpty()) {
            throw new IllegalArgumentException("해당 장바구니가 존재하지 않습니다.");
        }
        Orders temporaryOrder = orderService.createOrder(cartIds);

        // 세션에 임시 주문 정보를 저장
        httpSession.setAttribute("temporaryOrder", temporaryOrder);
        httpSession.setAttribute("cartIds", cartIds); // 장바구니 id 저장

        Object cartIdsAttribute = httpSession.getAttribute("cartIds");

        return ResponseEntity.ok("주문 임시 저장 완료");
    }

    /**
     * 주문서에서 입력받아 최종 주문 테이블 생성
     * @param request
     * @return
     */
    @PostMapping("/done")
    public ResponseEntity<Object> completeOrder(@Valid @RequestBody OrderDto request) {
        // 클라이언트로부터 받은 OrderDto 데이터를 매핑
        OrderDto orders = modelMapper.map(request, OrderDto.class);

        // 주문 확정 처리
        Orders completedOrder = orderService.orderConfirm(orders);

        // 주문 응답 DTO로 변환
        OrderResponseDto orderResponseDto = new OrderResponseDto(completedOrder);
        return ResponseEntity.ok(orderResponseDto);
    }

}
