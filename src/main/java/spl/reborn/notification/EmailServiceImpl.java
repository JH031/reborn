package spl.reborn.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    // 선택: 발신자 주소(없으면 spring.mail.username 사용)
    @Value("${mail.from:}")
    private String from;

    // 선택: 테스트용 강제 수신자(설정하면 모든 메일이 이 주소로 감)
    @Value("${mail.override.to:}")
    private String overrideTo;

    // spring.mail.username (발신자 기본값으로 사용)
    @Value("${spring.mail.username:}")
    private String mailUsername;

    // 간단한 이메일 형식 검사
    private static final Pattern EMAIL_RE = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Override
    public void sendReminder(String toEmail, String contentTitle, int offsetDays) {
        // 1) 수신자 결정 (override 우선)
        String to = (overrideTo != null && !overrideTo.isBlank()) ? overrideTo : toEmail;

        // 2) 수신자 유효성 검사
        if (to == null || to.isBlank()) {
            log.warn("[Email] Skip: empty recipient");
            return;
        }
        String toLower = to.toLowerCase();
        if (toLower.endsWith("@example.com") || !EMAIL_RE.matcher(to).matches()) {
            log.warn("[Email] Skip: invalid or test-domain recipient: {}", to);
            return;
        }

        // 3) 발신자 결정: mail.from → spring.mail.username 순
        String sender = (from != null && !from.isBlank()) ? from : mailUsername;

        String title = (contentTitle != null && !contentTitle.isBlank()) ? contentTitle : "복습 알림";
        String subject = "[복습 알림] " + title;
        String body = """
                오늘은 '%s'을(를) 복습하는 날입니다.
                설정한 %d일 후 복습 주기에 해당됩니다!

                👉 지금 복습하러 가기!
                """.formatted(title, offsetDays);

        // 4) 메일 전송 (TO 한 명만; CC/BCC 없음)
        SimpleMailMessage msg = new SimpleMailMessage();
        if (sender != null && !sender.isBlank()) {
            msg.setFrom(sender);
        }
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);

        mailSender.send(msg);
        log.info("[Email] Sent reminder to {}", to);
    }
}
