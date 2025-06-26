package JOO.jooshop;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/join")
    public String formJoin() {
        return "/members/join";
    }
    @GetMapping("/login")
    public String formLogin() {
        return "/members/login";
    }

    @GetMapping("/cart")
    public String formCart() {
        return "/carts/cart";
    }

    @GetMapping("/order")
    public String formOrder() {
        return "/orders/order";
    }

    @GetMapping("/tempOrder")
    public String formTempOrder() {
        return "/orders/tempOrder";
    }

    @GetMapping("/payment")
    public String formPayment() {
        return "/payments/payment";
    }
}
