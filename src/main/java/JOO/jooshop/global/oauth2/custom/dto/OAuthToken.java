package JOO.jooshop.global.oauth2.custom.dto;

import lombok.Data;

@Data
public class OAuthToken {
    private String access_token;
    private String token_type;
    private String refresh_token;
    private String scope;
    private long expires_in;
    private long refresh_token_expires_in;
}
