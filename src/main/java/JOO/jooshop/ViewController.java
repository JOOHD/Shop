package JOO.jooshop;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String formHome() {
        return "home";
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
