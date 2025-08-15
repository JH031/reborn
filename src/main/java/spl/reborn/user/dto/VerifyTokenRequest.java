package spl.reborn.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class VerifyTokenRequest {
    @NotBlank
    private String token;
}
