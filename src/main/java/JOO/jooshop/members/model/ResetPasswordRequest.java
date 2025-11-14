package JOO.jooshop.members.model;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String password;
    private String new_password;
    private String new_password_confirmation;
}
