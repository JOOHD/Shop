package JOO.jooshop.profiile.controller;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import JOO.jooshop.profiile.entity.Profiles;
import JOO.jooshop.profiile.model.MemberDTO;
import JOO.jooshop.profiile.model.MemberProfileDTO;
import JOO.jooshop.profiile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ProfileViewController {

    private final MemberRepositoryV1 memberRepository;
    private final ProfileRepository profileRepository;

    /**
     * MemberDTO
     *
     * 회원 엔티티(Member)를 직접 노출하지 않고,
     * 계정 기본 정보(이메일, 닉네임, 전화번호 등)만 담아 전달하는 DTO.
     * - 주로 로그인/회원 조회 등에서 사용
     * - Member → MemberDTO 변환 메서드 포함
     *
     * MemberProfileDTO
     *
     * 프로필 엔티티(Profiles) + 회원 기본 정보(MemberDTO)를 합친 DTO.
     * - 상세 프로필 화면 응답용
     * - 나이, 성별, 프로필 이미지, 자기소개 등 + 계정 기본 정보 포함
     */
    @GetMapping("/profile")
    public String profilePage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login"; // 로그인 안 되어 있으면 로그인 페이지로

        // 로그인 사용자 이메일 Member 조회
        Member member = memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("로그인된 사용자를 찾을 수 없습니다."));

        // profilesOpt : memberId 로 조회한 프로필 엔티티 Optional (없으면 빈 값 반환)
        Optional<Profiles> profilesOpt = profileRepository.findByMemberId(member.getId());

        MemberDTO memberDTO = MemberDTO.createMemberDto(member);

        MemberProfileDTO memberProfileDTO = profilesOpt
                .map(profiles -> MemberProfileDTO.createMemberProfileDto(profiles, memberDTO))
                .orElseGet(() -> new MemberProfileDTO(null, memberDTO, null, null, "", null, null, "", ""));

        model.addAttribute("member", memberProfileDTO);

        return "members/profile"; // profile.html
    }
}
/*
    if (profilesOpt.isPresent()) {
            Profiles profiles = profilesOpt.get();;

            // DTO 변환
            MemberDTO memberDTO = MemberDTO.createMemberDto(member);
            MemberProfileDTO memberProfileDTO = MemberProfileDTO.createMemberProfileDto(profiles, memberDTO);

            model.addAttribute("member", memberProfileDTO);
        } else {
            model.addAttribute("member", null);
        }
 */