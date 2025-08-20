package spl.reborn.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spl.reborn.notification.PwEmailService; // ★ 새 서비스 import
import spl.reborn.user.entity.PasswordResetToken;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.PasswordResetTokenRepository;
import spl.reborn.user.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;

    private final PwEmailService pwEmailService;
    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRE_MINUTES = 30;

    @Transactional
    public void sendResetLink(String userid, String email) {
        User user = userRepository.findByUseridAndEmailIgnoreCase(userid.trim(), email.trim())
                .orElseThrow(() -> new IllegalArgumentException("아이디/이메일이 일치하는 사용자가 없습니다."));

        String token = generateToken();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setToken(token);
        prt.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        prt.setUsed(false);
        tokenRepository.save(prt);

        String resetUrl = "http://localhost:3000/reset-password?token=" + token;
        String subject = "[REBORN] 비밀번호 재설정 안내";
        String body = """
                비밀번호 재설정을 요청하셨습니다.
                아래 링크에서 새 비밀번호를 설정해 주세요. (유효기간 %d분)

                %s
                """.formatted(EXPIRE_MINUTES, resetUrl);

        //  비밀번호 재설정 전용 메일 발송
        pwEmailService.sendText(user.getEmail(), subject, body);

        System.out.println("[RESET] email=" + user.getEmail() + ", token=" + token + ", url=" + resetUrl);
    }

    @Transactional(readOnly = true)
    public void verifyToken(String token) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
        if (prt.isUsed() || prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("토큰이 만료되었거나 이미 사용되었습니다.");
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
        if (prt.isUsed() || prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("토큰이 만료되었거나 이미 사용되었습니다.");
        }

        User user = prt.getUser();
        user.setPassword(newPassword);

        prt.setUsed(true);
        tokenRepository.save(prt);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
