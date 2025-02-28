package JOO.jooshop.global.authentication.oauth2.responsedto;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KakaoResponse implements OAuth2Response {

    private final Map<String, Object> attribute;

    public KakaoResponse(Map<String, Object> attribute) {
        this.attribute = attribute;
    }

    // Long 값을 Map으로 변환하는 메서드
    private Map<String, Object> convertLongToMap(Long idValue) {
        // 여기서는 예를 들어 Long 값을 그대로 사용하거나, 필요에 따라 Map으로
        // 예시로 빈 맵을 반환합니다.
        return new HashMap<>();
    }

    @Override
    public String getProvider() {

        return "kakao";
    }

    @Override
    public String getProviderId() {
        // "id" 값 가져오기
        return attribute != null && attribute.containsKey("id") ? attribute.get("id").toString() : null;
    }

    @Override
    public String getEmail() {
        return Optional.ofNullable(attribute) // Null 여부 검사 
                // .map(new Function<Map<String, Object>, Object>()
                // Function<T, R> interface, T = Map<String, Object> 입력타입, R = Object 반환타입
                .map(attrs -> attrs.get("kakao_account")) // 추출
                .filter(kakaoAccountObj -> kakaoAccountObj instanceof Map) // 타입 검사
                .map(kakaoAccountObj -> (Map<?, ?>) kakaoAccountObj) // 제네릭 와일드카드 ?(모든 타입 수용)
                .map(kakaoAccount -> kakaoAccount.get("email"))
                .filter(emailObj -> emailObj instanceof String)
                .map(Object::toString) // 문자열 반환 obj -> obj.toString()
                .orElse(null); // 최종 결과 반환
    }

    @Override
    public String getName() {
        return Optional.ofNullable(attribute)
                .map(attrs -> attrs.get("kakao_account"))
                .filter(kakaoAccountObj -> kakaoAccountObj instanceof Map)
                .map(kakaoAccountObj -> (Map<?, ?>) kakaoAccountObj)
                .map(kakaoAccount -> kakaoAccount.get("profile"))
                .filter(profileObj -> profileObj instanceof Map)
                .map(profileObj -> (Map<?, ?>) profileObj)
                .map(profile -> profile.get("nickname"))
                .filter(nicknameObj -> nicknameObj instanceof String)
                .map(Object::toString)
                .orElse(null);
    }

    /*
    @Override
    public String getEmail() {
        if (attribute != null && attribute.containsKey("kakao_account")) {
            Object kakaoAccountObj = attribute.get("kakao_account");
            if (kakaoAccountObj instanceof Map) {
                Map<?, ?> kakaoAccount = (Map<?, ?>) kakaoAccountObj;
                Object emailObj = kakaoAccount.get("email");
                if (emailObj instanceof String) {
                    return emailObj.toString();
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        if (attribute != null && attribute.containsKey("kakao_account")) {
            Object kakaoAccountObj = attribute.get("kakao_account");
            if (kakaoAccountObj instanceof Map) {
                Map<?, ?> kakaoAccount = (Map<?, ?>) kakaoAccountObj;
                Object profileObj = kakaoAccount.get("profile");
                if (profileObj instanceof Map) {
                    Map<?, ?> profile = (Map<?, ?>) profileObj;
                    // 실제 본명 : nickname
                    Object nicknameObj = profile.get("nickname");
                    if (nicknameObj instanceof String) {
                        return nicknameObj.toString();
                    }
                }
            }
        }
        return null;
    }
     */
}




















