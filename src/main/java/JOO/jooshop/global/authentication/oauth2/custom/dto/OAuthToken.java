package JOO.jooshop.global.authentication.oauth2.custom.dto;

import lombok.Data;

@Data
public class OAuthToken {
    // 외부 인증 서비스(예: 카카오, 구글)에서 받은 인증 관련 정보를 담는 DTO

    private String accessToken;
    private String token_type;
    private String refresh_token;
    private String scope;                   // 토큰이 적용된 범위.
    private long expires_in;                // 액세스 토큰의 만료 시간 (초 단위).
    private long refresh_token_expires_in;  // 리프레쉬 토큰의 만료 시간 (초 단위).
}
