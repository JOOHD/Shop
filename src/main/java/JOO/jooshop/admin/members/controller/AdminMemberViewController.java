package JOO.jooshop.admin.members.controller;

import JOO.jooshop.admin.members.model.AdminMemberDetailResponse;
import JOO.jooshop.admin.members.model.AdminMemberResponse;
import JOO.jooshop.admin.members.service.AdminMemberService;
import JOO.jooshop.members.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberViewController {

    private final AdminMemberService adminMemberService;

    /** 회원 목록 페이지 */
    @GetMapping("/list")
    public String memberListPage(Model model) {
        List<Member> members = adminMemberService.findAllMembers();
        model.addAttribute("members", members);
        return "admin/members/memberList";
    }

    /** 회원 상세 페이지 */
    @GetMapping("/detail/{memberId}")
    public String memberDetailPage(@PathVariable Long memberId, Model model) {
        Member memberDetail = adminMemberService.findMemberById(memberId);
        model.addAttribute("member", memberDetail);
        return "admin/members/memberDetail";
    }
}
