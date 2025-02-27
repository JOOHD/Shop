package JOO.jooshop.global.authentication.oauth2.responsedto;

import java.util.Map;

public class NaverResponse implements OAuth2Response {

    private final Map<String, Object> attribute;

    @SuppressWarnings("unchecked")
    public NaverResponse(Map<String, Object> attribute) {
        // "response" 키에 해당하는 값이 Map<String, Object> 타입인지 확인
        Object responseObj = attribute.get("response");
        if (responseObj instanceof Map) {
            this.attribute = (Map<String, Object>) responseObj;
        } else {
            throw new IllegalArgumentException("'response' key must have a value of type Map<String, Object>");
        }
    }

    @Override
    public String getProvider() {

        return "naver";
    }

    @Override
    public String getProviderId() {

        return attribute.get("id").toString();
    }

    @Override
    public String getEmail() {
        return attribute.get("email").toString();
    }

    @Override
    public String getName() {

        return attribute.get("name").toString();
    }
}
