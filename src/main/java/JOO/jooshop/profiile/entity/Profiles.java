package JOO.jooshop.profiile.entity;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.profiile.entity.enums.MemberAges;
import JOO.jooshop.profiile.entity.enums.MemberGender;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_profile")
public class Profiles {

    /**
     * 26.03.25 refactoring
     * 실제 FK owner는 profiles.member
     * root인 Member가 attachProfile(profile) 호출
     * 내부에서 profile.attachTo(this)로 FK owner 세팅
     *
     * DB 관계 변경의 진짜 주체는 자식의 FK 필드
     * aggregate 편입을 선언하는 주체는 root
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "profile_img_name")
    private String profileImgName;

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    private Profiles(String introduction, String profileImgName, String profileImgPath) {
        this.introduction = introduction;
        this.profileImgName = profileImgName;
        this.profileImgPath = profileImgPath;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /* 기본 프로필 생성 */
    public static Profiles createDefaultProfile() {
        return new Profiles("자기 소개를 수정해주세요.", null, null);
    }

    /* Aggregate Root(Member)가 child를 편입할 때 호출 */
    public void attachTo(Member member) {
        this.member = member;
    }

    public void changeIntroduction(String introduction) {
        this.introduction = introduction;
        touch();
    }

    public void changeProfileImage(String profileImgPath) {
        this.profileImgPath = profileImgPath;
        this.profileImgName = extractFileName(profileImgPath);
        touch();
    }

    public void changeMemberAge(MemberAges newAge) {
        this.memberAges = newAge;
        touch();
    }

    public void changeMemberGender(MemberGender newGender) {
        this.memberGender = newGender;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private String extractFileName(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        return imagePath.substring(imagePath.lastIndexOf("/") + 1);
    }
}