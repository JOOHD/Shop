package JOO.jooshop.members.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RefreshDto {

    private String refreshToken;

    private LocalDateTime expirationDate;

    public RefreshDto(String refreshToken, LocalDateTime expirationDate) {
        this.refreshToken = refreshToken;
        this.expirationDate = expirationDate;
    }

    public static RefreshDto createRefreshDto(String newRefreshToken, LocalDateTime expirationDateTime) {
        return new RefreshDto(newRefreshToken, expirationDateTime);
    }
}
