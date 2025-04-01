package JOO.jooshop.global.authentication.jwts.filters;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.RefreshRepository;
import JOO.jooshop.members.service.MemberService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class LoginFilter extends CustomJsonEmailPasswordAuthenticationFilter {

    private Long accessTokenExpirationPeriod = 3600L;

    private Long refreshTokenExpirationPeriod = 1209600L;

    private final MemberService memberService;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final ObjectMapper objectMapper;

    private static final String CONTENT_TYPE = "application/json"; // JSON 타입의 데이터로 오는 로그인 요청만 처리

    public LoginFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper, MemberService memberService, JWTUtil jwtUtil, RefreshRepository refreshRepository, ObjectMapper objectMapper1) {
        super(authenticationManager, objectMapper);
        this.memberService = memberService;
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
        this.objectMapper = objectMapper1;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {

        try {
            if (request.getContentType() == null || !request.getContentType().equals(CONTENT_TYPE)) {
                throw new AuthenticationServiceException("Authentication Content-Type not supported: " + request.getContentType());
            }
            String messageBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            // 자바 8 이상부터, TypeReference 를 통해 원하는 형(Type)을 넣어주지 않으면 경고문이 뜸. NullPointException 등등 (ex) get("email"), readValue("meesage") 등등 . 읽어오지 못할 경우도 생기기 때문
            // Map<String, String> usernamePasswordMap = objectMapper.readValue(messageBody, Map.class);

            Map<String, String> usernamePasswordMap = objectMapper.readValue(messageBody, new TypeReference<Map<String, String>>() {});

            // 클라이언트 요청에서 email, password 추출
            String email = usernamePasswordMap.get("email");
            String password = usernamePasswordMap.get("password");

            // 사용자 정보에서 isCertifyByMail 필드 확인
            Member member = memberService.validateDuplicatedEmail(email);

//            Member member = ByEmail.get();
//            if (!passwordEncoder.matches(password, member.getPassword())) {
//                throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
//            }
//            boolean isCertifyByEmail = member.isCertifyByMail();
//            log.info("[LoginFilter] 회원 이메일인증 여부 = " + isCertifyByMail);
            boolean isCertifyByMail = member.isCertifyByMail();
            if (!isCertifyByMail) {
                // 이메일이 인증되지 않은 경우 로그인 실패 처리
                throw new AuthenticationServiceException("Email is not certified yet. 이메일 인증이 되지 않았습니다. ");
            }

            // Principal(유저이메일), Credentials(비밀번호), Authority(권한) 등의 정보
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);
            log.info(String.valueOf(authToken.toString()));
            return this.getAuthenticationManager().authenticate(authToken);
        } catch (AuthenticationServiceException e) {
            log.info("로그인에 실패했습니다. 원인: " + e.getMessage());
            throw e;
        }
    }

    @Override
    // 로그인 성공 시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
    public void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException
    {
        // 개발 단계에서 로그확인. 배포 후 : 없앨 예정
        log.warn("개발 단계에서 유저에 대한 정보를 확인하는 로그입니다. 배포 시 삭제해야 합니다 ! [24.04.06 김성우]");
        log.info("로그인에 성공했습니다.");
        log.info("유저 메일: " + authentication.getName());
        log.info("유저 권한: " + authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        String email = authentication.getName();
        Member memberByEmail = memberService.validateDuplicatedEmail(email);
        String memberId = memberByEmail.getId().toString(); // 토큰에 넣을때, 문자열로 넣습니다.

        // 권한을 문자열로 변환
        String role = extractAuthority(authentication);

        // 토큰 종류(카테고리), 유저이름, 역할 등을 페이로드에 담는다.
        String newAccess = jwtUtil.createAccessToken("access", memberId, role);
        String newRefresh = jwtUtil.createRefreshToken("refresh", memberId, role);

        // [Refresh 토큰 - DB 에서 관리합니다.] 리프레쉬 토큰 권리권한이 서버에 있다.
        saveOrUpdateRefreshEntity(memberByEmail, newRefresh);

        // [response.data] 에 Json 형태로 accessToken 과 refreshToken 을 넣어주는 방식
        addResponseDataV3(response, newAccess, newRefresh, email);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {

        log.info("로그인에 실패했습니다. ");
        //로그인 실패시 401 응답 코드 반환
        response.setStatus(401);
        response.getWriter().write("로그인에 실패했습니다! ");
        super.unsuccessfulAuthentication(request, response, failed);
    }

    // 사용자의 권한 정보를 가져옴

    private String extractAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER"); // 기본 권한 설정. [따로 설정하지 않았을때]
    }
}
