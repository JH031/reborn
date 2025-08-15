package spl.reborn.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ResetPasswordRequest {
    @NotBlank
    private String token;

    @NotBlank
    private String newPassword; // ★ 평문 저장 (요청사항)
}
