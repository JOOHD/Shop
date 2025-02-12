package JOO.jooshop.global.jwts.service;
import JOO.jooshop.global.jwts.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    public String getRefreshAuthorization(HttpServletRequest request) {
        return CookieUtil.getCookieValue(request, "refreshAuthorization");
    }
}
