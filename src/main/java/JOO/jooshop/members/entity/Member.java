package JOO.jooshop.members.entity;

import JOO.jooshop.address.entity.Addresses;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.entity.enums.SocialType;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.wishList.entity.WishList;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "member", uniqueConstraints = {
        @UniqueConstraint(columnNames = "social_id") // 중복 소셜ID 방지
})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @NotBlank
    private String email;

    private String password;

    private String username;

    private String nickname;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role")
    @JsonProperty("member_role")
    private MemberRole memberRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type")
    @JsonProperty("social_type")
    private SocialType socialType;

    @NotBlank
    @Column(name = "social_id", unique = true)
    @JsonProperty("social_id")
    private String socialId;

    @Column(name = "joined_at")
    @JsonProperty("joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "is_banned")
    private boolean banned = false;

    @Column(name = "is_accountExpired")
    private boolean accountExpired = false;

    @Column(name = "is_passwordExpired")
    private boolean passwordExpired = false;

    @Column(name = "is_admin")
    private boolean admin = false;

    @Column(name = "is_certified_email")
    private boolean certifiedByEmail = false;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<WishList> wishLists = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<PaymentHistory> paymentHistories = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Addresses> addresses = new ArrayList<>();

    // ========================== 정적 생성 메서드 ==========================

    /**
     * [OAuth2 기반 소셜 로그인 회원 생성]
     * - 회원가입이 아닌, 소셜 로그인 인증 성공 후 호출
     * - 인증 정보를 DB에 저장할 때 사용됨
     */
    public static Member createSocialMember(String email, String username,
                                            MemberRole role, SocialType socialType, String socialId) {
        return Member.builder()
                .email(email)
                .username(username)
                .memberRole(role)
                .socialType(socialType)
                .socialId(socialId)
                .certifiedByEmail(true)     // 소셜 로그인은 이메일 인증 생략
                .active(true)
                .build();
    }

    /**
     * [이메일 회원가입 요청 시, 기본 회원 객체 생성]
     * - 인증 메일 발송 직후, DB에 임시 계정으로 저장할 때 사용
     * - 패스워드는 없는 상태이며, 인증 이후 최종 정보 업데이트 필요
     */
    public static Member createEmailMember(String email) {
        return Member.builder()
                .email(email)
                .memberRole(MemberRole.USER)
                .socialType(SocialType.GENERAL)
                .certifiedByEmail(false)    // 인증 전 상태
                .active(true)
                .build();
    }

    /**
     * [이메일+패스워드 기반 일반 회원 생성]
     * - 사용자 입력 정보로 일반 회원 생성
     */
    public static Member createGeneralMember(String email, String username, String nickname,
                                             String password, String phone, String socialId) {
        return Member.builder()
                .email(email)
                .username(username)
                .nickname(nickname)
                .password(password)
                .phone(phone)
                .memberRole(MemberRole.USER)
                .socialType(SocialType.GENERAL)
                .socialId(socialId)
                .certifiedByEmail(false)
                .active(true)
                .build();
    }

    /**
     * [관리자 계정 생성]
     * - admin 권한 부여 및 기본 인증 완료 처리
     */
    public static Member createAdminMember(String email, String username, String nickname,
                                           String password, String phone, String socialId) {
        return Member.builder()
                .email(email)
                .username(username)
                .nickname(nickname)
                .password(password)
                .phone(phone)
                .memberRole(MemberRole.ADMIN)
                .socialType(SocialType.GENERAL)
                .socialId(socialId)
                .certifiedByEmail(true)
                .admin(true)
                .active(true)
                .build();
    }

    /**
     * [프로필 전용 객체 반환]
     * - 비밀번호, 전화번호 등 민감 정보 제외하고 사용자 정보만 전달할 때 사용
     * - 보통 API 응답 DTO로 변환 전 단계
     */
    public static Member createProfileMember(Member member) {
        return Member.builder()
                .email(member.getEmail())
                .username(member.getUsername())
                .nickname(member.getNickname())
                .memberRole(member.getMemberRole())
                .socialType(member.getSocialType())
                .socialId(member.getSocialId())
                .certifiedByEmail(member.isCertifiedByEmail())
                .build();
    }

// ========================== 도메인 메서드 ==========================

    /**
     * [소셜 회원 정보 업데이트]
     * - 동일 이메일 또는 socialId로 로그인한 사용자가 있을 경우 정보 갱신
     */
    public void updateOAuth2Member(Member newOAuth2Member) {
        this.email = newOAuth2Member.getEmail();
        this.username = newOAuth2Member.getUsername();
        this.memberRole = newOAuth2Member.getMemberRole();
        this.socialType = newOAuth2Member.getSocialType();
        this.socialId = newOAuth2Member.getSocialId();
        this.certifiedByEmail = newOAuth2Member.isCertifiedByEmail();
    }

    /**
     * [비밀번호 암호화]
     * - 회원 가입 or 비밀번호 변경 시에만 호출
     */
    public void passwordEncode(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }

    /**
     * [이메일 인증 완료 처리]
     * - 인증 URL을 통해 인증 성공 시 호출됨
     */
    public void certifyByEmail() {
        this.certifiedByEmail = true;
    }

    /**
     * [테스트용 Setter]
     * - 이메일 인증 여부를 직접 설정 (일반적으로는 사용하지 않음)
     */
    public void setCertifiedByEmail(boolean certifiedByEmail) {
        this.certifiedByEmail = certifiedByEmail;
    }

    /**
     * [관리자 권한 부여]
     */
    public void grantAdminRole() {
        this.admin = true;
    }

    /**
     * [계정 활성화 처리]
     */
    public void activate() {
        this.active = true;
    }

    /**
     * [계정 비활성화 처리]
     * - 탈퇴 혹은 임시 정지 상태로 설정할 때 사용
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * [회원 정지 처리]
     * - 운영자 판단 하에 사용 정지
     */
    public void ban() {
        this.banned = true;
    }

    /**
     * [정지 해제]
     */
    public void unban() {
        this.banned = false;
    }

    /**
     * [계정 만료 처리]
     * - 예: 장기 미접속자 처리 등
     */
    public void expireAccount() {
        this.accountExpired = true;
    }

    /**
     * [계정 만료 해제]
     */
    public void renewAccount() {
        this.accountExpired = false;
    }

    /**
     * [비밀번호 만료 처리]
     * - 주기적 비밀번호 변경 요구 시 사용
     */
    public void expirePassword() {
        this.passwordExpired = true;
    }

    /**
     * [비밀번호 만료 해제]
     */
    public void renewPassword() {
        this.passwordExpired = false;
    }

    /**
     * [비밀번호 변경]
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    /**
     * [닉네임 변경]
     */
    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

}
