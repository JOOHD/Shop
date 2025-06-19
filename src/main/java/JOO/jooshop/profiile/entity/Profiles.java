package JOO.jooshop.profiile.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.profiile.entity.enums.MemberAges;
import JOO.jooshop.profiile.entity.enums.MemberGender;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_profile")
public class Profiles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Setter
    @Column(name = "profile_img_name")
    private String profileImgName;
    @Setter
    @Column(name = "profile_img_path")
    private String profileImgPath;

    @Lob
    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_ages")
    private MemberAges memberAges;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_gender")
    private MemberGender memberGender;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Profiles(Member member, String introduction, String profileImgName, String profileImgPath) {
        this.member = member;
        this.introduction = introduction;
        this.profileImgName = profileImgName;
        this.profileImgPath = profileImgPath;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // **도메인 주도 설계(DDD)**
    // Setter 대신 의미 있는 메서드로 상태 변경을 캡슐화
    // 엔티티 내부에서 상태 변경(변수 변경)을 안전하게 다루기 위해 설정된 메서드들

    // 최초 회원가입 시, 기본적으로 만들어주는 프로필. (CustomOAuth2UserServiceV2)
    public static Profiles createMemberProfile(Member member) {
        return new Profiles(member, "자기 소개를 수정해주세요. ", null, null);
    }

    public void updateDateTime(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void updateProfileImgPath(String profileImgPath) {
        this.profileImgPath = profileImgPath;
    }

    public void updateProfileImgName(String profileImgName) {
        this.profileImgName = profileImgName;
    }

    public void updateIntroduction(String introduction) {
        this.introduction = introduction;
    }

    // 경로와 파일명 설정 메서드 호출
    public void setProfileImage(byte[] imageBytes) {
        setProfileImagePathAndImageName(new String(imageBytes));
    }

    // 경로에서 파일명을 추출
    private void setProfileImagePathAndImageName(String imagePath) {
        this.profileImgPath = imagePath;
        this.profileImgName = this.profileImgPath.substring(this.profileImgPath.lastIndexOf("/") + 1);
    }

    public void updateMemberAge(MemberAges newAge) {
        this.memberAges = newAge;
    }

    public void updateMemberGender(MemberGender newGender) {
        this.memberGender = newGender;
    }
}
