package JOO.jooshop.global.mail.service;

import JOO.jooshop.global.authentication.jwts.utils.JWTUtil;
import JOO.jooshop.global.mail.entity.CertificationEntity;
import JOO.jooshop.global.mail.repository.CertificationRepository;
import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.repository.MemberRepositoryV1;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailMemberService {

    private final JWTUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final MemberRepositoryV1 memberRepository;
    private final CertificationRepository certificationRepository;

    @Value("${spring.backend.url}")
    private String backendUrl;

    @Value("${spring.mail.username}")
    private String senderEmail;

    /**
     * 이메일 인증 요청 시 호출됨
     * - 기존 인증 토큰 제거
     * - 새로운 토큰 발급 및 저장
     * - 인증 링크를 포함한 이메일 발송
     */
    @Transactional
    public void sendEmailVerification(String email) throws Exception {
        // (1) 동일 이메일의 이전 인증 요청 제거 (1인 1토큰 정책)
        certificationRepository.deleteByEmail(email);

        // (2) 새로운 인증 토큰 생성 (JWTUtil로 발급)
        String token = jwtUtil.createEmailToken(email);

        // (3) 토큰을 CertificationEntity에 저장
        CertificationEntity cert = CertificationEntity.create(email, token);
        certificationRepository.save(cert);

        // (4) 인증 링크 생성 및 이메일 전송
        String link = backendUrl + "/auth/verify?token=" + token;
        sendEmail(email, link);
    }

    /**
     * 실제 인증 메일 전송 로직
     */
    private void sendEmail(String to, String link) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        message.setFrom(new InternetAddress(senderEmail));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject("[JooShop] 이메일 인증을 완료해주세요");
        message.setText("이메일 인증을 위해 아래 링크를 클릭하세요:\n" + link, "utf-8");

        mailSender.send(message);
    }

    /**
     * 이메일 인증 링크 클릭 시 호출
     * - 토큰 검증 및 만료 확인
     * - 인증 대상 회원 조회
     * - 이메일 인증 처리 (certifiedByEmail = true)
     * - 토큰은 사용 후 삭제 (1회용 처리)
     * - 인증 완료된 회원 객체 반환
     */
    @Transactional
    public Member verifyEmailAndReturnMember(String token) {
        // (1) 토큰으로 CertificationEntity 조회
        CertificationEntity cert = certificationRepository.findByToken(token)
                .orElseThrow(() -> new UsernameNotFoundException("유효하지 않은 인증 토큰입니다."));

        // (2) 만료 여부 확인
        if (cert.isExpired()) {
            throw new IllegalArgumentException("인증 토큰이 만료되었습니다.");
        }

        // (3) 이메일 기준으로 회원 조회
        Member member = memberRepository.findByEmail(cert.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일의 회원이 존재하지 않습니다."));

        // (4) 인증 처리 (Member 도메인 메서드 사용)
        member.certifyByEmail();

        // (5) 인증 토큰 삭제 (1회용 사용 후 폐기)
        certificationRepository.delete(cert);

        // (6) 인증 완료된 회원 반환
        return member;
    }

    /**
     * 단순한 인증 여부만 확인하고 true/false 반환
     * - 사용처: 프론트 확인용 API 등에서 사용 가능
     */
    @Transactional
    public boolean verifyEmailToken(String token) {
        // (1) 토큰 존재 여부 확인
        Optional<CertificationEntity> certOpt = certificationRepository.findByToken(token);
        if (certOpt.isEmpty()) return false;

        // (2) 인증 대상 회원 조회
        String email = certOpt.get().getEmail();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // (3) 인증 처리 및 토큰 삭제
        member.certifyByEmail();
        certificationRepository.delete(certOpt.get());
        return true;
    }
}
