package JOO.jooshop.members.entity;


import JOO.jooshop.members.model.RefreshDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "refresh")
public class Refresh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_id")
    private Long id;

    @Column(name = "refresh_token")
    private String refreshToken;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "member_id")
    private Member member;

//    @Column(columnDefinition = "DATETIME", name = "REFRESH_EXPIRATION") // H2Database의 경우
    @Column(columnDefinition = "TIMESTAMP") // MySQL의 경우
    private LocalDateTime expiration;

    public Refresh(Member member, String refreshToken, LocalDateTime expiration) {
        this.member = member;
        this.refreshToken = refreshToken;
        this.expiration = expiration;
    }

    public void updateRefreshToken(RefreshDto refreshDto) {
        this.refreshToken = refreshDto.getRefreshToken(); // this.~ -> entity 필드
        this.expiration = refreshDto.getExpirationDate();
    }
}
