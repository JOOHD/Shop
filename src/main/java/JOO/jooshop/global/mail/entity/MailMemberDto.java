package JOO.jooshop.global.mail.entity;

import JOO.jooshop.members.entity.Member;
import lombok.Getter;

@Getter
public class MailMemberDto {
    private String email;
    private String username;
    private String nickname;
    private String token;

    public MailMemberDto(String email, String username, String nickname, String token) {
        this.email = email;
        this.username = username;
        this.nickname = nickname;
        this.token = token;
    }

    // 멤버 객체의 이메일을 사용하여 MailMemberDto 객체를 생성한다.
    public MailMemberDto createFromMemberByEmail(Member member) {
        return new MailMemberDto(member.getEmail(), this.username, this.nickname, this.token);
    }

    // 멤버 객체의 토큰을 사용하여 MailMemberDto 객체를 생성한다.
    public MailMemberDto updateFromMemberByToken(Member member) {
        return new MailMemberDto(this.email, this.username, this.nickname, member.getToken());
    }
}
