package JOO.jooshop.order.controller;

import JOO.jooshop.cart.entity.Cart;
import JOO.jooshop.cart.repository.CartRepository;
import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.members.entity.Member;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    
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
    public ResponseEntity<String> createOrder(@RequestBody OrderDto orderDto,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        // orderDto 내부에서 cartIds 꺼내기
        List<Long> cartIds = orderDto.getCartIds();

        // createOrder 메서드 호출
        orderService.createOrder(cartIds, orderDto);
        return ResponseEntity.ok("임시 주문이 Redis에 저장되었습니다.");
    }

    /**
     * 주문서에서 입력받아 최종 주문 테이블 생성
     * @return
     */
    @PostMapping("/done")
    public ResponseEntity<Object> doneOrder(@RequestBody OrderDto orderDto,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderService.confirmOrder(orderDto);
        return ResponseEntity.ok("주문이 확정되어 DB에 저장되었습니다.");
    }

}
