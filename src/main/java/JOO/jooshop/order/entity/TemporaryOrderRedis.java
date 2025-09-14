package JOO.jooshop.order.entity;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash("TemporaryOrder") // Redis key : TemporaryOrder:{id}
public class TemporaryOrderRedis implements Serializable {
    /**
     * 목적 : Redis 에 저장하기 위한 전용 DTO/Entity
     * 장점 : 짧은 시간만 필요/ 세션 처럼 사용자별로 관리
     * Postman  에서도 UUID 만 넘겨주면 잘 작동한다. 서버 간 세션 공유 문제도 해결
     * 저장 위치 : RDB 가 아닌 Redis
     * JPA Entity 아님, JPA 와는 무관, 일반 DTO 아님, 클라이언트 요청/응답을 DTO 가 아니라, Redis 캐싱용 내부 모델
       -> Redis 는 문자열/해시/리스트 등 단순 자료구조만 저장 가능
            -> 자바 객체를 그대로 저장할 수 없니까 직렬화 필요 (Serializable implements)
     * 정리 : Redis는 임시/세션성 데이터 저장에 최적. DB보다 가볍고 빠름.
     */

    @Id
    private String id;                  // Redis key: tempOrder:{memberId}
    private Long memberId;              // 구매자 ID
    private String username;            // 로그인용 주문자명
    private String ordererName;         // 화면에 표시 될 주문자명
    private String phoneNumber;         // 전화번호
    private List<Long> cartIds;         // 장바구니 ID
    private List<String> productNames;  // 상품명
    private List<String> productSizes;  // 상품 사이즈
    private List<String> productImages; // 상품 이미지
    private List<Integer> productQuantities; // 상품 수량 추가
    private BigDecimal totalPrice;      // 총 결제 금액

    @TimeToLive(unit = TimeUnit.MINUTES)
    private long expiration = 30; // TTL: 30분 후 자동 삭제
}
