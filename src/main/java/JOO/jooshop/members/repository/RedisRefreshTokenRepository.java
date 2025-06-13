package JOO.jooshop.members.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class RedisRefreshTokenRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final long expirationSeconds = 60 * 60 * 24 * 7; // 7일

    public RedisRefreshTokenRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String memberId, String refreshToken) {
        redisTemplate.opsForValue().set(generateKey(memberId), refreshToken, Duration.ofSeconds(expirationSeconds));
    }

    public String findByMemberId(String memberId) {
        return redisTemplate.opsForValue().get(generateKey(memberId));
    }

    public void delete(String memberId) {
        redisTemplate.delete(generateKey(memberId));
    }

    public boolean exists(String memberId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(generateKey(memberId)));
    }

    private String generateKey(String memberId) {
        return "refresh_token:" + memberId;
    }
}
