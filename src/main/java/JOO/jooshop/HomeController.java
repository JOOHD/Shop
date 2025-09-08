package JOO.jooshop;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/") // header.html login modal 로그인 시, 변경 관련
    public String home(Model model, Authentication authentication) {
        model.addAttribute("loggedIn", authentication != null && authentication.isAuthenticated());
        return "home"; // home.html 렌더링
    }

}
