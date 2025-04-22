package JOO.jooshop.global.authentication.jwts.service;
import JOO.jooshop.global.authentication.jwts.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    public String getRefreshAuthorization(HttpServletRequest request) {
        String refreshToken = CookieUtil.getCookieValue(request, "refreshAuthorization");

        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        // 여기서는 "Bearer " 붙이지 않고, 필터에서 처리
        return refreshToken;
    }
}
