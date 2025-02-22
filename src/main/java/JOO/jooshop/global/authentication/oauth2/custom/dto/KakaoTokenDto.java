package JOO.jooshop.global.authentication.oauth2.custom.dto;

import lombok.Data;

@Data
public class KakaoTokenDto {
    /*
        카카오에서 보내는 access token 을 매핑하는 클래스다.
        인가코드 받아와서 HTTP Request 완성한 후 서버에 띄워보고 정확한 필드명을 구해보자

        참고: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#request-token
     */

    private String token_type;               // 토큰 타입 (Bearer)
    private String access_token;             // 액세스 토큰 (API 요청 시 사용)
    private String id_token;                 // ID 토큰 (OIDC 기반 로그인 시 사용)
    private int expires_in;                  // 액세스 토큰 만료 시간(초 단위)
    private String refresh_token;            // 리프레시 토큰 (액세스 토큰 갱신 시 사용)
    private int refresh_token_expires_in;    // 리프레시 토큰 만료 시간(초 단위)
    private String scope;                    // 접근 권한 범위 (옵션)
}
