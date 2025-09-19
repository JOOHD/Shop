package JOO.jooshop.order.entity;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
@Builder
@RedisHash("TemporaryOrder") // Redis key : TemporaryOrder:{id}
public class TemporaryOrderRedis implements Serializable {
    /**
     * 목적 : Redis에 저장하기 위한 전용 캐싱 모델 (TemporaryOrder 전용)
     * 장점 :
     *   1. 임시/세션성 데이터를 사용자별로 빠르게 저장/조회 가능
     *   2. Redis에 직렬화된 객체로 저장 → 서버 간 세션 공유 문제 해결
     *   3. Postman 등 외부 테스트에서도 UUID 키만 있으면 동작
     *
     * 특징 :
     *   - RDB에 저장되는 JPA Entity와는 무관
     *   - 일반 DTO와 달리 Redis 전용 내부 모델
     *   - Redis는 문자열/해시/리스트 등 단순 자료구조만 지원 → Serializable로 직렬화 필요
     *
     * 리팩토링 후 개선점 :
     *   1. 모든 리스트 필드(cartIds, productNames 등)를 기본값(empty list)으로 초기화 → 조회 시 NullPointerException 방지
     *   2. Builder 및 생성자에서 null 입력 시 자동으로 빈 리스트로 변환 → 안전한 객체 생성 보장
     *   3. Redis 조회 시 `.stream()` 등 리스트 연산 시 null 체크 불필요 → 코드 간결화
     *
     * TTL : 30분 후 자동 삭제
     *
     * 요약 :
     *   Redis는 임시/세션성 데이터 저장에 최적화, DB보다 가볍고 빠름.
     *   리팩토링 후 안전성과 안정성이 강화됨.
     */

    @Id
    private String id;                  // Redis key: tempOrder:{memberId}
    private Long memberId;              // 구매자 ID
    private String username;            // 로그인용 주문자명
    private String ordererName;         // 화면에 표시 될 주문자명
    private String phoneNumber;         // 전화번호

    // ====== 리스트 필드 초기화 ======
    @Builder.Default
    private List<Long> cartIds = new ArrayList<>();
    @Builder.Default
    private List<String> productNames = new ArrayList<>();
    @Builder.Default
    private List<String> productSizes = new ArrayList<>();
    @Builder.Default
    private List<String> productImages = new ArrayList<>();
    @Builder.Default
    private List<Integer> productQuantities = new ArrayList<>();

    private BigDecimal totalPrice;      // 총 결제 금액

    @TimeToLive(unit = TimeUnit.MINUTES)
    private long expiration = 30; // TTL: 30분 후 자동 삭제

    // ====== 안전한 빌더 생성자 ======
    @Builder
    public TemporaryOrderRedis(String id,
                               Long memberId,
                               String username,
                               String ordererName,
                               String phoneNumber,
                               List<Long> cartIds,
                               List<String> productNames,
                               List<String> productSizes,
                               List<String> productImages,
                               List<Integer> productQuantities,
                               BigDecimal totalPrice,
                               long expiration) {
        this.id = id;
        this.memberId = memberId;
        this.username = username;
        this.ordererName = ordererName;
        this.phoneNumber = phoneNumber;
        this.cartIds = cartIds != null ? cartIds : new ArrayList<>();
        this.productNames = productNames != null ? productNames : new ArrayList<>();
        this.productSizes = productSizes != null ? productSizes : new ArrayList<>();
        this.productImages = productImages != null ? productImages : new ArrayList<>();
        this.productQuantities = productQuantities != null ? productQuantities : new ArrayList<>();
        this.totalPrice = totalPrice;
        this.expiration = expiration;
    }
}
