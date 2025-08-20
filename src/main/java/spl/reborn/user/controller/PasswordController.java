package spl.reborn.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spl.reborn.user.dto.ResetPasswordRequest;
import spl.reborn.user.dto.SendResetLinkRequest;
import spl.reborn.user.dto.VerifyTokenRequest;
import spl.reborn.user.service.PasswordResetService;

@RestController
@RequestMapping("/api/password")
public class PasswordController {

    private final PasswordResetService passwordResetService;

    public PasswordController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/send-link")
    @Operation(
            summary = "비밀번호 재설정 링크 발송",
            description = "아이디와 이메일이 일치하는 계정에 대해 재설정 토큰을 생성하고 이메일로 링크를 발송합니다."
    )
    public ResponseEntity<?> sendLink(@RequestBody SendResetLinkRequest request) {
        passwordResetService.sendResetLink(request.getUserid(), request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-token")
    @Operation(
            summary = "재설정 토큰 검증",
            description = "토큰의 유효성(만료/사용 여부 포함)을 확인하여 재설정 페이지 진입 가능 여부를 판단합니다."
    )
    public ResponseEntity<?> verify(@RequestBody VerifyTokenRequest request) {
        passwordResetService.verifyToken(request.getToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    @Operation(
            summary = "새 비밀번호 설정",
            description = "유효한 토큰으로 새 비밀번호를 저장합니다. (요청에 따라 암호화 없이 평문 저장)"
    )
    public ResponseEntity<?> reset(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
