package JOO.jooshop.members.model.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RefreshRequest {

    private String refreshToken;

    private LocalDateTime expirationDate;

    public RefreshRequest(String refreshToken, LocalDateTime expirationDate) {
        this.refreshToken = refreshToken;
        this.expirationDate = expirationDate;
    }

    public static RefreshRequest createRefreshDto(String newRefreshToken, LocalDateTime expirationDateTime) {
        return new RefreshRequest(newRefreshToken, expirationDateTime);
    }
}
