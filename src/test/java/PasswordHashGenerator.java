import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin"; // 원하는 비밀번호
        String hashedPassword = encoder.encode(rawPassword);
        System.out.println(hashedPassword);
    }
}
