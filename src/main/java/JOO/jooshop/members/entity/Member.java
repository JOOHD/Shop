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

    @Column(name = "email_token")
    @JsonProperty("email_token")
    private String token;

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
     * 소셜 로그인 회원 생성
     */
    public static Member createSocialMember(String email, String username,
                                            MemberRole role, SocialType socialType, String socialId) {
        return Member.builder()
                .email(email)
                .username(username)
                .memberRole(role)
                .socialType(socialType)
                .socialId(socialId)
                .certifiedByEmail(true)
                .active(true)
                .build();
    }

    /**
     * 이메일 회원가입 시 기본 회원 생성
     */
    public static Member createEmailMember(String email, String token) {
        return Member.builder()
                .email(email)
                .memberRole(MemberRole.USER)
                .socialType(SocialType.GENERAL)
                .token(token)
                .certifiedByEmail(false)
                .active(true)
                .build();
    }

    /**
     * 일반 회원가입 처리 (패스워드 회원)
     */
    public static Member createGeneralMember(String email, String nickname, String password,
                                             String token, String socialId) {
        return Member.builder()
                .email(email)
                .nickname(nickname)
                .password(password)
                .memberRole(MemberRole.USER)
                .socialType(SocialType.GENERAL)
                .socialId(socialId)
                .token(token)
                .certifiedByEmail(false)
                .active(true)
                .build();
    }

    /**
     * 관리자 계정 생성
     */
    public static Member createAdminMember(String email, String nickname, String password,
                                           String token, String socialId) {
        return Member.builder()
                .email(email)
                .nickname(nickname)
                .password(password)
                .memberRole(MemberRole.ADMIN)
                .socialType(SocialType.GENERAL)
                .socialId(socialId)
                .token(token)
                .certifiedByEmail(true)
                .admin(true)
                .active(true)
                .build();
    }

    /**
     * 민감 정보 제외한 프로필용 객체 생성
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
    // 도메인 메서드 : "서비스 단에서 결정된 비즈니스 행동을, 실제로 수행하는 책임 있는 객체"

    public void updateOAuth2Member(Member newOAuth2Member) {
        this.email = newOAuth2Member.getEmail();
        this.username = newOAuth2Member.getUsername();
        this.memberRole = newOAuth2Member.getMemberRole();
        this.socialType = newOAuth2Member.getSocialType();
        this.socialId = newOAuth2Member.getSocialId();
        this.certifiedByEmail = newOAuth2Member.isCertifiedByEmail();
    }

    public void passwordEncode(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }

    // 인증 처리 ( true로 변경하는 setter 성격의 도메인 메서드)
    public void certifyByEmail() {
        this.certifiedByEmail = true;
    }

    // 권한 처리
    public void grantAdminRole() {
        this.admin = true;
    }

    // 계정 활성/비활성화
    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    // 정지 처리
    public void ban() {
        this.banned = true;
    }

    public void unban() {
        this.banned = false;
    }

    // 계정 만료 처리
    public void expireAccount() {
        this.accountExpired = true;
    }

    public void renewAccount() {
        this.accountExpired = false;
    }

    // 비밀번호 만료 처리
    public void expirePassword() {
        this.passwordExpired = true;
    }

    public void renewPassword() {
        this.passwordExpired = false;
    }

    // 비밀번호 변경
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    // 닉네임 변경
    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    // Getter 역할 함수 (일관된 이름)
    public MemberRole getRole() {
        return this.memberRole;
    }

    // 필드의 현재 값을 조회(get) 하는 getter 메서드
    public boolean isCertifiedByEmail() {
        return this.certifiedByEmail;
    }

    public boolean isAccountExpired() {
        return this.accountExpired;
    }

    public boolean isPasswordExpired() {
        return this.passwordExpired;
    }
}
