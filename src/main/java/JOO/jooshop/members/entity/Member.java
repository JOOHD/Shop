package JOO.jooshop.members.entity;

import JOO.jooshop.global.time.BaseEntity;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.entity.enums.SocialType;
import JOO.jooshop.profiile.entity.Profiles;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_social_id", columnNames = "social_id")
})
public class Member extends BaseEntity {

    /**
     * 26.03.25 refactoring
     * 1. 회원 계정의 중심
     * 2. 계정 상태 / 인증 여부 / 비밀번호 / 닉네임 / 프로필 소유
     * 3. Profiles를 편입하는 root
     * 4. @setter 제거 -> domain 메서드
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "phone", nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false)
    private MemberRole memberRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false)
    private SocialType socialType;

    @Column(name = "social_id")
    private String socialId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_banned", nullable = false)
    private boolean banned;

    @Column(name = "is_account_expired", nullable = false)
    private boolean accountExpired;

    @Column(name = "is_password_expired", nullable = false)
    private boolean passwordExpired;

    @Column(name = "is_admin", nullable = false)
    private boolean admin;

    @Column(name = "is_certified_email", nullable = false)
    private boolean certifiedByEmail;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private Profiles profile;

    private Member(
            String email,
            String password,
            String username,
            String nickname,
            String phoneNumber,
            MemberRole memberRole,
            SocialType socialType,
            String socialId,
            boolean certifiedByEmail,
            boolean admin
    ) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.memberRole = memberRole;
        this.socialType = socialType;
        this.socialId = socialId;
        this.certifiedByEmail = certifiedByEmail;
        this.admin = admin;

        this.active = true;
        this.banned = false;
        this.accountExpired = false;
        this.passwordExpired = false;
        this.joinedAt = LocalDateTime.now();
    }

    /* 일반 회원 가입 */
    public static Member registerGeneral(
            String email,
            String encodedPassword,
            String username,
            String nickname,
            String phoneNumber,
            String socialId
    ) {
        return new Member(
                email,
                encodedPassword,
                username,
                nickname,
                phoneNumber,
                MemberRole.USER,
                SocialType.GENERAL,
                socialId,
                false,
                false
        );
    }

    /* 관리자 생성 */
    public static Member registerAdmin(
            String email,
            String encodedPassword,
            String username,
            String nickname,
            String phoneNumber,
            String socialId
    ) {
        return new Member(
                email,
                encodedPassword,
                username,
                nickname,
                phoneNumber,
                MemberRole.ADMIN,
                SocialType.GENERAL,
                socialId,
                true,
                true
        );
    }

    /* OAuth2 소셜 회원 생성 */
    public static Member registerSocial(
            String email,
            String username,
            MemberRole role,
            SocialType socialType,
            String socialId
    ) {
        return new Member(
                email,
                "",
                username,
                username,
                "",
                role,
                socialType,
                socialId,
                true,
                role == MemberRole.ADMIN
        );
    }

    /* Profile child 편입 */
    public void attachProfile(Profiles profile) {
        this.profile = profile;
        profile.attachTo(this);
    }

    /* 이메일 인증 완료 */
    public void verifyEmail() {
        this.certifiedByEmail = true;
    }

    /* OAuth2 로그인 시 최신 정보 반영 */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordExpired = false;
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void changePhoneNumber(String newPhoneNumber) {
        this.phoneNumber = newPhoneNumber;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void ban() {
        this.banned = true;
    }

    public void unban() {
        this.banned = false;
    }

    public void expireAccount() {
        this.accountExpired = true;
    }

    public void renewAccount() {
        this.accountExpired = false;
    }

    public void expirePassword() {
        this.passwordExpired = true;
    }

    public void renewPassword() {
        this.passwordExpired = false;
    }

    public void grantAdminRole() {
        this.memberRole = MemberRole.ADMIN;
        this.admin = true;
    }

    public void updateSocialLoginInfo(String email, String username, String socialId, SocialType socialType) {
        this.email = email;
        this.username = username;
        this.socialId = socialId;
        this.socialType = socialType;
        this.certifiedByEmail = true;
    }

    @Transient
    public String getStatusText() {
        if (banned) return "정지";
        if (!active) return "비활성화";
        if (accountExpired) return "계정 만료";
        if (passwordExpired) return "비밀번호 만료";
        if (admin) return "관리자";
        return "활성";
    }
}