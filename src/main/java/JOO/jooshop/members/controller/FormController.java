package JOO.jooshop.members.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FormController {

    @GetMapping("/join")
    public String formJoin() {
        return "members/join";
    }
    @GetMapping("/login")
    public String formLogin() {
        return "members/login"; // templates/members/login.html 랜더링
    }
}
