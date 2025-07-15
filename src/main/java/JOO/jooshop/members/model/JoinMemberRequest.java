package JOO.jooshop.members.model;

import JOO.jooshop.address.model.AddressesReqeustDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class JoinMemberRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String username;

    @NotBlank
    private String nickname;

    @NotBlank
    private String phone;

    @NotBlank
    private String password1;

    @NotBlank
    private String password2;

    private AddressesReqeustDto address;
}
