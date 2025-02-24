package JOO.jooshop.global.mail.entity;

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

    private final JavaMailSender mailSender;
    private final MemberRepositoryV1 memberRepository;

    /*
        http://backend-url/auth/verify?token=...
        backend-url 클릭 시, 이메일 인증이 완료되는 구조
     */
    @Value("${backend.url") 
    private String backendUrl;

    @Transactional
    public void sendEmailVerification(Member member) throws Exception {

        String receiverMail = member.getEmail();
        MimeMessage message = mailSender.createMimeMessage();

        message.addRecipients(Message.RecipientType.TO, receiverMail); // 받는 분 메일 추가
        message.setSubject("JOO 회원가입 이메일 인증입닏. "); // 제목

        String body = "<div>"
                + "<h1> 안녕하세요. JOO Shopping mall 입니다!<h1>"
                + "<br>"
                + "<p>JOO 회원가입을 축하드리며, 저희 서비스를 이용해주셔서 감사합니다. <p>"
                + "<p>아래 링크를 클릭하면 이메일 인증이 완료됩니다.<p>"
                + "<a href='" + backendUrl + "/auth/verify?token=" + member.getToken() + "'>인증 링크</a>"
                + "<p>즐거운 쇼핑 되세요.!<p>"
                + "</div>";
        message.setText(body, "utf-8", "html"); // 내용, charset 타입, subtype
        message.setFrom(new InternetAddress("hddong728@naver.com", "JOO_ADMIN")); // 보내는 사람
        mailSender.send(message);
    }

    @Transactional
    public Member updateByVerifyToken(String token) {
        Optional<Member> optionalMember = memberRepository.findByToken(token);

        // 검색된 회원이 있을 경우, 업데이트 수행
        if (optionalMember.isPresent()) {
            // 회원 정보를 업데이트
            Member member = optionalMember.get();
            log.info("member email token = " + member.getToken());
            member.certifyByEmail(); // isVerified = true

            // 변경된 이메일 인증 여부, 이메일 토큰을 DB에 반영
            return memberRepository.save(member);
        } else {
            throw new UsernameNotFoundException("해당 토큰을 가진 멤버가 존재하지 않습니다!");
        }
        // 업데이트된 또는 검색된 회원을 반환.
    }
}
