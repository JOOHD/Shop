package JOO.jooshop.members.entity;

import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.entity.enums.SocialType;
import JOO.jooshop.address.entity.Addresses;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.entity.enums.SocialType;
import com.fasterxml.jackson.annotation.JsonProperty;
import JOO.jooshop.payment.entity.PaymentHistory;
import JOO.jooshop.wishList.entity.WishList;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member", uniqueConstraints = {
        @UniqueConstraint(columnNames = "social_id")
})
public class Member {

    @Id
    @Column(name = "member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String email;

    private String password;

    private String username;

    private String nickname;

    private String phone;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "member_role")
    @JsonProperty("member_role")
    private MemberRole memberRole;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "social_type")
    @JsonProperty("social_type")
    private SocialType socialType;

    @Column(name = "social_id", unique = true)
    @NotBlank
    @JsonProperty("social_id")
    private String socialId; // Provider + prividerId 형식

    @Column(name = "joined_at")
    @JsonProperty("joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "is_banned")
    private boolean isBanned = false;

    @Column(name = "is_accountExpired")
    private boolean isAccountExpired = false;

    @Column(name = "is_passwordExpired")
    private boolean isPasswordExpired = false;

    @Column(name = "is_admin")
    private boolean isAdmin = false;

    @Column(name = "email_token")
    @JsonProperty("email_token")
    private String token; // email 인증 시, 사용될 토큰

    @Column(name = "is_certified_email")
    private boolean isCertifyByMail = false;

    @OneToMany(mappedBy = "member")
    private List<WishList> wishLists = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<PaymentHistory> paymentHistories = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Addresses> addresses = new ArrayList<>();

    // 1. 생성자를 통해 멤버 생성
    public Member(String email, String password, String username, String nickname, MemberRole memberRole, SocialType socialType, String socialId, String token, boolean isCertifyByMail) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.nickname = nickname;
        this.memberRole = memberRole;
        this.socialType = socialType;
        this.socialId = socialId;
        this.token = token;
        this.isCertifyByMail = isCertifyByMail;
    }
    // 2. Profile 에 멤버를 담을 때, 민감한 정보(비밀번호, 토큰) 등을 담아주지 않기 위한 생성자
    public Member(String email, String username, String nickname, MemberRole memberRole, SocialType socialType, String socialId, boolean isCertifyByMail) {
        this.email = email;
        this.username = username;
        this.nickname = nickname;
        this.memberRole = memberRole;
        this.socialType = socialType;
        this.socialId = socialId;
        this.isCertifyByMail = isCertifyByMail;
    }

    // Social Member 생성
    public static Member createSocialMember(String email, String username, MemberRole memberRole, SocialType socialType, String socialId) {
        return new Member(email, null, username, null, memberRole, socialType, socialId, null, true);
    }

    // Email 인증을 진행한 멤버를 생성할때 사용
    public static Member createEmailMember(String email, String token) {
        return new Member(email, null, null, null, MemberRole.USER, SocialType.GENERAL, null, token, false);
    }

    // General Member 생성
    public static Member createGeneralMember(String email, String nickname, String password, String token, String socialId) {
        return new Member(email, password, null, nickname, MemberRole.USER, SocialType.GENERAL, socialId, token, false);
    }

    // Admin Member 생성
    public static Member createAdminMember(String email, String nickname, String password, String token, String socialId) {
        return new Member(email, password, null, nickname, MemberRole.ADMIN, SocialType.GENERAL, socialId, token, true);
    }

    public static Member createProfileMember(Member member) {
        return new Member(member.getEmail(), member.getUsername(), member.getNickname(), member.getMemberRole(), member.getSocialType(), member.getSocialId(), member.isCertifyByMail);
    }


    // 새로운 멤버 객체 생성하여 반환하는 메서드
    public void updateOAuth2Member(Member newOAuth2Member) {
        this.email = newOAuth2Member.getEmail();
        this.username = newOAuth2Member.getUsername();
        this.memberRole = newOAuth2Member.getMemberRole();
        this.socialType = newOAuth2Member.getSocialType();
        this.socialId = newOAuth2Member.getSocialId();
        this.isCertifyByMail = newOAuth2Member.isCertifyByMail();
        // 필요한 경우에 따라 다른 필드도 업데이트할 수 있습니다.
    }

    public static Member createOAuth2Member(String email, String username, SocialType socialType, String socialId) {
        return new Member(email, null, username, null, MemberRole.USER, socialType, socialId, null, true);
    }

    // 유저 권한 설정 메소드
    public void authorizeUser() {
        this.memberRole = MemberRole.USER;
    }

    // 관리자 권한 설정 메소드
    public void authorizeAdmin() {
        this.memberRole = MemberRole.ADMIN;
    }

    // 비밀번호 암호화 메소드
    public void passwordEncode(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }
    
    // 이메일 인증 여부를 업데이트하는 메서드
    public void certifyByEmail() {
        this.isCertifyByMail = true;
    }
    // 이메일 인증 토큰 업데이트
    public void updateMemberByToken(String token) {
        this.token = token;
    }
    // 관리자 활성화
    public void verifyAdminUser() {
        this.isAdmin = true;
    }
    // 회원 비활성화
    public void deActivateMember() {
        this.isActive = false;
    }
    // 회원 활성화 여부 가져오기
    public boolean getIsActive() {
        return isActive;
    }
    // 회원 계정 정지 여부 가져오기
    public boolean getIsBanned() {
        return isBanned;
    }
//    // 회원 계정 만료 여부 가져오기
//    public boolean getIsAccountExpired() {
//        return this.getIsAccountExpired(); // 자기 자신을 계속 호출 → 무한 재귀 호출 발생
//    }
//    // 회원 비밀번호 만료 여부 가져오기
//    public boolean getIsPasswordExpired() {
//        return this.getIsPasswordExpired();
//    }

    // 회원 비활성화
    public void activateMember() {
        this.isActive = true;
    }
    // 비밀번호 재설정
    public void resetPassword(String newPassword) {
        this.password = newPassword;
    }
    // 닉네임 수정
    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

}
