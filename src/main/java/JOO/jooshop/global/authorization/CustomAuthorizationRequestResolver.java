package JOO.jooshop.global.authorization;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.Builder;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository repo, String authorizationRequestBaseUri) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, authorizationRequestBaseUri);

        // 커스터마이저 등록 — PKCE 파라미터 제거
        this.defaultResolver.setAuthorizationRequestCustomizer(builder -> {
            // PKCE 파라미터 제거
            builder
                    .additionalParameters(params -> {
                        params.remove(PkceParameterNames.CODE_CHALLENGE);
                        params.remove(PkceParameterNames.CODE_CHALLENGE_METHOD);
                    });
            // attributes 도 제거해도 무방
            builder.attributes(attrs -> {
                attrs.remove(PkceParameterNames.CODE_CHALLENGE);
                attrs.remove(PkceParameterNames.CODE_CHALLENGE_METHOD);
            });
        });
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return defaultResolver.resolve(request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return defaultResolver.resolve(request, clientRegistrationId);
    }
}
