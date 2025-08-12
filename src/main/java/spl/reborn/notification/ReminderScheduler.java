package spl.reborn.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;

    @Scheduled(cron = "0 */10 * * * *") //
    public void run() {
        reminderService.sendDueReminders();
    }
}
