package JOO.jooshop.global.authentication.oauth2.custom.service;


import JOO.jooshop.global.authentication.oauth2.custom.entity.CustomOAuth2User;
import JOO.jooshop.global.authentication.oauth2.responsedto.GoogleResponse;
import JOO.jooshop.global.authentication.oauth2.responsedto.KakaoResponse;
import JOO.jooshop.global.authentication.oauth2.responsedto.NaverResponse;
import JOO.jooshop.global.authentication.oauth2.responsedto.OAuth2Response;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.entity.enums.SocialType;
import JOO.jooshop.members.model.OAuthUserDTO;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserServiceV1 extends DefaultOAuth2UserService {

    private final MemberRepositoryV1 memberRepository;
    private final ProfileRepository profileRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("[OAuth2] OAuth2UserRequest 받은 clientRegistrationId: {}", userRequest.getClientRegistration().getRegistrationId());

        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("[OAuth2] OAuth2User attributes: {}", oAuth2User.getAttributes());

        OAuth2Response oAuth2Response = createOAuth2Response(userRequest.getClientRegistration().getRegistrationId(), oAuth2User.getAttributes());
        if (oAuth2Response == null) {
            log.error("[OAuth2] 해당 소셜 로그인 타입에 대한 Response 생성 실패");
            return null;  // 또는 예외 던지기
        }
        log.info("[OAuth2] OAuth2Response 생성 완료: {}", oAuth2Response);

        Member memberByOAuth2Response = saveMemberAndProfile(oAuth2Response);
        log.info("[OAuth2] 멤버 DB 저장/조회 완료: {}", memberByOAuth2Response.getEmail());

        CustomOAuth2User customOAuth2User = createOAuth2User(memberByOAuth2Response);
        log.info("[OAuth2] CustomOAuth2User 생성 완료");

        return customOAuth2User;
    }

    private OAuth2Response createOAuth2Response(String registrationType, Map<String, Object> attributes) {
        // 소셜 타입별 DTO 생성 팩토리 (switch expression 사용 - 깔끔)
        return switch (registrationType) {
            case "naver" -> new NaverResponse(attributes);
            case "google" -> new GoogleResponse(attributes);
            case "kakao" -> new KakaoResponse(attributes);
            default -> null;  // null 반환 대신 예외를 던지거나 Optional로 처리하는게 좋음
        };
    }

    private Member saveMemberAndProfile(OAuth2Response oAuth2Response) {
        // ProviderId 및 socialId 구성
        String ProviderId = oAuth2Response.getProviderId();
        String SocialId = oAuth2Response.getProvider() +  "_" + ProviderId;

        // 소셜 타입 매핑
        SocialType socialType = mapRegistrationTypeToSocialType(oAuth2Response.getProvider());

        // 이메일, 이름 추출
        String email = oAuth2Response.getEmail();
        String username = oAuth2Response.getName();

        //  기존 멤버 조회
        Optional<Member> OptionalMember = memberRepository.findBySocialId(SocialId);

        if (OptionalMember.isPresent()) {
            Member joinMember = OptionalMember.get();

            // 프로필이 없으면 생성 (중복 코드: 프로필 생성 로직이 else와 동일)
            Optional<Profiles> optionalProfiles = profileRepository.findByMemberId(joinMember.getId());
            if (optionalProfiles.isPresent()) {
                return joinMember;
            } else {
                Profiles profile = Profiles.createMemberProfile(joinMember);
                profileRepository.save(profile);
                return joinMember;
            }

        } else {
            // 신규 멤버 저장 및 프로필 생성 (중복 코드: 프로필 생성 부분 반복됨)
            Member newOAuth2Member = Member.createSocialMember(email, username, MemberRole.USER, socialType, SocialId);
            memberRepository.save(newOAuth2Member);

            Profiles profile = Profiles.createMemberProfile(newOAuth2Member);
            profileRepository.save(profile);

            return newOAuth2Member;
        }
    }

    private CustomOAuth2User createOAuth2User(Member member) {
        // Member -> OAuthUserDTO 변환 후 CustomOAuth2User 생성
        OAuthUserDTO userDTO = OAuthUserDTO.createOAuthUserDTO(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                member.getMemberRole(),
                member.getSocialType(),
                member.getSocialId(),
                true);
        return new CustomOAuth2User(userDTO);
    }

    private SocialType mapRegistrationTypeToSocialType(String registrationType) {
        // registrationType -> SocialType 변환
        return switch (registrationType) {
            case "naver" -> SocialType.NAVER;
            case "google" -> SocialType.GOOGLE;
            case "kakao" -> SocialType.KAKAO;
            default -> SocialType.GENERAL;
        };
    }
}

