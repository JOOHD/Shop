package JOO.jooshop.global.authentication.oauth2.custom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SocialResponse {
    // 이 토큰과 이메일 등의 정보를 클라이언트에게 전달하는 응답 DTO
    
    private String accessToken;
    private String refreshToken;
    private String email;

}