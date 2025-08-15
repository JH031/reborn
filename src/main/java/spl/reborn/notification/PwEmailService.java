package spl.reborn.notification;

public interface PwEmailService {
    void sendText(String toEmail, String subject, String body);
}
