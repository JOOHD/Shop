package JOO.jooshop.global.authentication.oauth2.custom.entity;

import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.entity.enums.SocialType;
import JOO.jooshop.members.model.OAuthUserDTO;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User, Serializable {

    private final OAuthUserDTO oAuthUserDTO;

    // email, username, role, socialType, socialId
    private String email;
    @Getter
    private String username;
    @Getter
    private MemberRole role;
    @Getter
    private SocialType socialType;
    @Getter
    private String socialId;

    public CustomOAuth2User(OAuthUserDTO oAuthUserDTO) {
        this.oAuthUserDTO = oAuthUserDTO;
        this.email = oAuthUserDTO.getEmail();
        this.username = oAuthUserDTO.getUsername();
        this.role = oAuthUserDTO.getRole();
        this.socialType = oAuthUserDTO.getSocialType();
        this.socialId = oAuthUserDTO.getSocialId();
    }

    /*
        OAuth2 로그인은 여러 소셜 서비스(구글, 네이버, 카카오 등)와 연동될 수 있다.
        Spring Security 는 OAuth2User 인터페이스로 통일해서 다루려 한다.
        로그인한 사용자의 'OAuth 제공자'가 넘겨준 정보들(프로필 데이터)을 Map<String, Object> 형태로 반환
        {
            "email": "user@example.com",
            "username": "John Doe",
            "socialType": "GOOGLE",
            "socialId": "123456789"
        }
        getAttributes()는 이 사용자 정보(프로필)를 통합해서 꺼내볼 수 있는 창구
    */
    @Override
    public Map<String, Object> getAttributes() {
        return oAuthUserDTO.toMap();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Collection<GrantedAuthority> collection = new ArrayList<>();

        collection.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return oAuthUserDTO.getRole().toString();
            }
        });
        return collection;
    }

    @Override
    public String getName() {
        // 로그인 이용자의 email
        return email;
    }

    @Override
    public <A> A getAttribute(String name) {
        return OAuth2User.super.getAttribute(name);
    }

    public Long getMemberId() { return oAuthUserDTO.getMemberId(); }
}
