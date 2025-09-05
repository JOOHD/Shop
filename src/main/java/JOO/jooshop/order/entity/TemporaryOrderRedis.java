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
     * Postman  에서도 UUID 만 넘겨주면 잘 작동한다. 서버 간 세션 공유 문제도 해결
     * 사용 위치 : Spring Data Redis 의 @RedishHash 를 사용하는 경우
     * 저장 위치 : RDB 가 아닌 Redis
     * JPA Entity 아님, JPA 와는 무관
     * 일반 DTO 아님, 클라이언트 요청/응답을 DTO 가 아니라, Redis 캐싱용 내부 모델
     */

    @Id
    private String id; // Redis 키 값 (UUID 등으로 생성)

    private Long memberId;
    private String username;
    private String phoneNumber;
    private BigDecimal totalPrice;

    private List<Long> cartIds;
    private List<String> productNames;   // 상품명
    private List<String> productSizes;   // 사이즈
    private List<String> productImages;  // 이미지 URL

    @TimeToLive(unit = TimeUnit.MINUTES)
    private long expiration = 30; // TTL: 30분 후 자동 삭제
}
