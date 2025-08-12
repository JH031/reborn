package spl.reborn.notification;

public interface EmailService {
    void sendReminder(String toEmail, String contentTitle, int offsetDays);
}
