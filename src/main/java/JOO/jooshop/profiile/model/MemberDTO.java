package JOO.jooshop.profiile.model;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.entity.enums.MemberRole;
import JOO.jooshop.members.entity.enums.SocialType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class MemberDTO {

    private Long id;
    private String email;
    private String username;
    private String nickname;
    private String phone;
    private MemberRole member_role;
    private SocialType social_type;
    private Boolean is_active;
    private Boolean is_admin;
    private Boolean is_certified_email;
    private String joined_at;

    public MemberDTO
            (Long id,
             String email,
             String username,
             String nickname,
             String phone,
             MemberRole member_role,
             SocialType social_type,
             Boolean is_active,
             Boolean is_admin,
             Boolean is_certified_email,
             String joined_at)
    {
        this.id = id;
        this.email = email;
        this.username = username;
        this.nickname = nickname;
        this.phone = phone;
        this.member_role = member_role;
        this.social_type = social_type;
        this.is_active = is_active;
        this.is_admin = is_admin;
        this.is_certified_email = is_certified_email;
        this.joined_at = joined_at;
    }

    public static MemberDTO createMemberDto(Member member) {
        return new MemberDTO(member.getId(),
                             member.getEmail(),
                             member.getUsername(),
                             member.getNickname(),
                             member.getPhone(),
                             member.getMemberRole(),
                             member.getSocialType(),
                             member.isActive(),
                             member.isAdmin(),
                             member.isCertifiedByEmail(),
                             // joinedAt null 체크 후 변환
                             member.getJoinedAt() != null ? member.getJoinedAt().toString() : ""
        );
    }
}
