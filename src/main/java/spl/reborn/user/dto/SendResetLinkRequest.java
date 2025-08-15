package spl.reborn.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SendResetLinkRequest {
    @NotBlank
    private String userid;

    @NotBlank @Email
    private String email;
}
