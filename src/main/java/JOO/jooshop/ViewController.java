package JOO.jooshop;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/") // header.html login modal 로그인 시, 변경 관련
    public String home(Model model, Authentication authentication) {
        model.addAttribute("loggedIn", authentication != null && authentication.isAuthenticated());
        return "home"; // home.html 렌더링
    }

    @GetMapping("/join")
    public String formJoin() {
        return "members/join";
    }

    @GetMapping("/order")
    public String formOrder() {
        return "orders/order";
    }

    @GetMapping("/tempOrder")
    public String formTempOrder() {
        return "orders/tempOrder";
    }

    @GetMapping("/payment")
    public String formPayment() {
        return "payments/payment";
    }
}
