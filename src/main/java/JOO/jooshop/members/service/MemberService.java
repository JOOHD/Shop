package JOO.jooshop.members.service;

import JOO.jooshop.members.entity.Member;
import JOO.jooshop.members.model.LoginRequest;
import JOO.jooshop.members.repository.MemberRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.security.auth.login.CredentialNotFoundException;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // write(PUT,DELETE,INSERT) 작업은 X
@Slf4j
public class MemberService {

    private final MemberRepositoryV1 memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public Member joinMember(Member member) {
        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new ExistingMemberException();
        }
        Member newMember = Member.builder()
                .email(member.getEmail())
                .nickname(member.getNickname())
                .password(member.getPassword())
                .token(member.getToken())
                .socialId(member.getSocialId())
                .build();

        newMember.activateMember();
        return memberRepository.save(newMember);
    }

    @Transactional
    public Member memberLogin(LoginRequest loginRequest) throws UserPrincipalNotFoundException, CredentialNotFoundException {
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UserPrincipalNotFoundException("User not found with email: " + loginRequest.getEmail()));

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            log.info("Invalid password for email: {} ", loginRequest.getEmail());
            throw new CredentialNotFoundException("Invalid password");
        }

        return member;
    }

    @Transactional
    public Member validateDuplicatedEmail(String email) {
        Optional<Member> optionalMember = memberRepository.findByEmail(email);
        return optionalMember.orElseThrow(
                () -> new UserNotFoundByEmailException("No user found with this email: " + email));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class ExistingMemberException extends IllegalStateException {
        public ExistingMemberException() {
            super("이미 존재하는 회원입니다.");
        }
    }

    public static class UserNotFoundByEmailException extends RuntimeException {
        public UserNotFoundByEmailException(String message) {
            super(message);
        }
    }
}






