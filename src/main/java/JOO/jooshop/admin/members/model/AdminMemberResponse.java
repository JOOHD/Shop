package JOO.jooshop.admin.members.model;

import JOO.jooshop.members.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMemberResponse {

    private Long id;
    private String email;
    private String username;
    private String nickname;
    private String phoneNumber;
    private String memberRole;
    private boolean certifiedByEmail;
    private boolean active;

    /**
     * Member 엔티티 → AdminMemberResponse DTO 변환
     */
    public static AdminMemberResponse toDto(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                member.getNickname(),
                member.getPhoneNumber(),
                member.getMemberRole().name(),
                member.isCertifiedByEmail(),
                member.isActive()
        );
    }
}
