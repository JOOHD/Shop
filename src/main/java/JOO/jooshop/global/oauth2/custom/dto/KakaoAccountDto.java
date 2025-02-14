package JOO.jooshop.global.oauth2.custom.dto;

import lombok.Data;

@Data
public class KakaoAccountDto {

    /*
        @sierrah
        [Kakao] 현재 mument 에서의 동의 항목
        필수 - 닉네임 (profile_nickname)
        선택 - 카카오계정 이메일 (account_email)
     */
    public Long id;                     // 회원번호 (카카오 고유 ID), *Required*
    public String connected_at;         // 서비스에 연결된 시각 (ISO 8601 형식, UTC 기준)
    public String properties;           // 사용자의 프로필 정보 (JSON 형태로 넘어옴, 닉네임 등 포함)
    public KakaoAccount kakao_account;  // 카카오 계정 관련 상세 정보 객체

    @Data
    public class KakaoAccount {
        public Boolean profile_nickname_needs_agreement; // 닉네임 제공 동의 필요 여부
        public Boolean email_needs_agreement;            // 이메일 제공 동의 필요 여부
        public Boolean is_email_valid;                   // 이메일 유효 여부
        public Boolean is_email_verified;                // 이메일 인증 여부
        public Boolean has_email;                        // 2메일 보유 여부

        public String email;                             // 사용자 이메일
        public String name;                              // 사용자 이름 (동의 항목 설정 시 제공)
        public KakaoProfile profile;                     // 프로필 관련 정보 객체

        @Data
        public class KakaoProfile {
            public String nickname;                      // 사용자 닉네임
        }
    }
}
