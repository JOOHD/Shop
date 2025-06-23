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

/**
 * 회원(Member) 도메인 엔티티 클래스
 * - 일반 회원 및 소셜 회원 통합 관리
 * - 인증 관련 상태 (정지, 비활성화, 만료 등) 포함
 * - 회원 관련 연관 엔티티와의 관계 포함 (주소, 결제, 찜 등)
 */
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

    // 사용자 권한 (USER, ADMIN, SELLER 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "member_role")
    @JsonProperty("member_role")
    private MemberRole memberRole;

    // 소셜 가입 여부 (GOOGLE, KAKAO, NAVER, GENERAL)
    @Enumerated(EnumType.STRING)
    @Column(name = "social_type")
    @JsonProperty("social_type")
    private SocialType socialType;

    // 소셜 ID 또는 고유 가입 식별자
    @NotBlank
    @Column(name = "social_id", unique = true)
    @JsonProperty("social_id")
    private String socialId;

    // 가입 일시
    @Column(name = "joined_at")
    @JsonProperty("joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    // 계정 활성화 여부 (true: 정상, false: 비활성화)
    @Column(name = "is_active")
    private boolean active = true;

    // 계정 정지 여부
    @Column(name = "is_banned")
    private boolean banned = false;

    // 계정 만료 여부
    @Column(name = "is_accountExpired")
    private boolean accountExpired = false;

    // 비밀번호 만료 여부
    @Column(name = "is_passwordExpired")
    private boolean passwordExpired = false;

    // 관리자 권한 여부 (UI 제어용 또는 내부 제어용)
    @Column(name = "is_admin")
    private boolean admin = false;

    // 이메일 인증 토큰
    @Column(name = "email_token")
    @JsonProperty("email_token")
    private String token;

    // 이메일 인증 여부
    @Column(name = "is_certified_email")
    private boolean certifiedByEmail = false;

    // 연관관계: 찜 목록
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<WishList> wishLists = new ArrayList<>();

    // 연관관계: 결제 내역
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<PaymentHistory> paymentHistories = new ArrayList<>();

    // 연관관계: 배송지 주소
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Addresses> addresses = new ArrayList<>();

    // ========================== 생성자 ==========================

    /**
     * 전체 필드를 포함한 생성자
     */
    public Member(String email, String password, String username, String nickname,
                  MemberRole memberRole, SocialType socialType, String socialId,
                  String token, boolean certifiedByEmail) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.nickname = nickname;
        this.memberRole = memberRole;
        this.socialType = socialType;
        this.socialId = socialId;
        this.token = token;
        this.certifiedByEmail = certifiedByEmail;
    }

    /**
     * 민감 정보 제외한 프로필용 생성자
     */
    public Member(String email, String username, String nickname,
                  MemberRole memberRole, SocialType socialType,
                  String socialId, boolean certifiedByEmail) {
        this.email = email;
        this.username = username;
        this.nickname = nickname;
        this.memberRole = memberRole;
        this.socialType = socialType;
        this.socialId = socialId;
        this.certifiedByEmail = certifiedByEmail;
    }

    // ========================== 정적 생성 메서드 ==========================

    public static Member createSocialMember(String email, String username,
                                            MemberRole memberRole, SocialType socialType, String socialId) {
        return new Member(email, null, username, null, memberRole, socialType, socialId, null, true);
    }

    public static Member createEmailMember(String email, String token) {
        return new Member(email, null, null, null, MemberRole.USER, SocialType.GENERAL, null, token, false);
    }

    public static Member createGeneralMember(String email, String nickname, String password,
                                             String token, String socialId) {
        return new Member(email, password, null, nickname, MemberRole.USER, SocialType.GENERAL, socialId, token, false);
    }

    public static Member createAdminMember(String email, String nickname, String password,
                                           String token, String socialId) {
        return new Member(email, password, null, nickname, MemberRole.ADMIN, SocialType.GENERAL, socialId, token, true);
    }

    public static Member createProfileMember(Member member) {
        return new Member(
                member.getEmail(),
                member.getUsername(),
                member.getNickname(),
                member.getMemberRole(),
                member.getSocialType(),
                member.getSocialId(),
                member.isCertifiedByEmail()
        );
    }

    // ========================== 도메인 메서드 ==========================

    /**
     * OAuth2 로그인 시 기존 회원 정보 업데이트
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
     * 비밀번호 암호화
     */
    public void passwordEncode(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }

    /**
     * 이메일 인증 완료 처리
     */
    public void certifyByEmail() {
        this.certifiedByEmail = true;
    }

    /**
     * 관리자 권한 부여
     */
    public void verifyAdminUser() {
        this.admin = true;
    }

    /**
     * 회원 비활성화
     */
    public void deactivateMember() {
        this.active = false;
    }

    /**
     * 회원 활성화
     */
    public void activateMember() {
        this.active = true;
    }

    /**
     * 정지 여부 변경
     */
    public void ban() {
        this.banned = true;
    }

    /**
     * 회원 정지 해제
     */
    public void unban() {
        this.banned = false;
    }

    /**
     * 회원 계정 만료
     */
    public boolean accountExpired() {
        return this.accountExpired;
    }

    /**
     * 비밀번호 변경
     */
    public void resetPassword(String newPassword) {
        this.password = newPassword;
    }

    /**
     * 닉네임 수정
     */
    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    /**
     * 역할 반환
     */
    public MemberRole getRole() {
        return this.memberRole;
    }

    /**
     * 이메일 인증 여부 반환
     */
    public boolean isCertifiedByEmail() {
        return certifiedByEmail;
    }
}
