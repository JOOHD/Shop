package JOO.jooshop.order.controller;

import JOO.jooshop.global.authentication.jwts.entity.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderViewController {

    @GetMapping("/tempOrder")
    public String tempOrderPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        // 로그인한 사용자 ID를 HTML 로 전달
        model.addAttribute("memberId", userDetails.getMemberId());
        return "orders/tempOrder";
    }
}

