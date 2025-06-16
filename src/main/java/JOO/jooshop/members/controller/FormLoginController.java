package JOO.jooshop.members.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class FormLoginController {

    @GetMapping("/login")
    public String formLogin() {
        return "members/login"; // templates/members/login.html 랜더링
    }
}
