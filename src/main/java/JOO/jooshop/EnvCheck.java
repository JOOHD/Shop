package JOO.jooshop;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class EnvCheck {

    @PostConstruct
    public void checkEnv() {
        System.out.println("IMP_API_KEY from System.getenv(): " + System.getenv("IMP_API_KEY"));
    }
}
