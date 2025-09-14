package JOO.jooshop.order.controller;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import JOO.jooshop.order.entity.TemporaryOrderRedis;
import JOO.jooshop.order.model.OrderDto;
import JOO.jooshop.order.repository.RedisOrderRepository;
import JOO.jooshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;


@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderApiController {

    // API : 보안 검증 용도 , View : HTML 렌더링 & JS 데이터 전달 용도

    private final OrderService orderService;
    private final RedisOrderRepository redisOrderRepository;

    /**
     * 로그인한 사용자의 Redis 임시 주문 정보를 조회
     */
    @GetMapping("/temp/{memberId}")
    public ResponseEntity<TemporaryOrderRedis> getTemporaryOrder(
            @PathVariable Long memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // @AuthenticationPrincipal : 현재 로그인한 사용자의 인증 정보를 가져오는
        if (!userDetails.getMemberId().equals(memberId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Redis에서 "tempOrder:{memberId}" 키 조회
        String redisKey = "tempOrder:" + memberId;
        TemporaryOrderRedis tempOrder = redisOrderRepository.findById(redisKey)
                .orElseThrow(() -> new NoSuchElementException("임시 주문 정보가 없습니다."));

        // 데이터 반환: 주문자 정보 + 선택 상품 정보 + 총액 → 화면 렌더링용
        return ResponseEntity.ok(tempOrder);
    }

    /**
     * 장바구니 선택 후 주문 생성 -> Redis에 임시 주문 저장
     */
    @PostMapping("/create")
    public ResponseEntity<String> createOrder(@RequestBody OrderDto orderDto,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderService.createOrder(orderDto.getCartIds(), orderDto);
        return ResponseEntity.ok("임시 주문이 Redis에 저장되었습니다.");
    }

    /**
     * 주문서 작성 후 결제 -> 실제 주문 DB 저장
     */
    @PostMapping("/confirm")
    public ResponseEntity<Object> confirmOrder(@RequestBody OrderDto orderDto,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderService.confirmOrder(orderDto);
        return ResponseEntity.ok("주문이 확정되어 DB에 저장되었습니다.");
    }

}
