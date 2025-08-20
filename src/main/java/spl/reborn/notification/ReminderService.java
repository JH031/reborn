package spl.reborn.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReminderService {

    private final UserRepository userRepository;
    private final ReminderRepository reminderRepository;
    private final EmailService emailService;

    private static final int[] OFFSETS = {1, 4, 7, 14, 30};
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime DEFAULT_SEND_TIME = LocalTime.of(9, 0);



    public void createForStudy(long userId, Long contentId, String contentTitle, LocalDate studyDate, String imageUrl) {
        LocalDate base = (studyDate != null) ? studyDate : LocalDate.now(KST);
        String title = (contentTitle == null || contentTitle.isBlank()) ? "복습 알림" : contentTitle;
        scheduleOffsetsFromDate(userId, contentId, title, base); // 이미지 미사용
    }


    public void createForStudy(long userId, Long contentId, String contentTitle, LocalDate studyDate) {
        LocalDate base = (studyDate != null) ? studyDate : LocalDate.now(KST);
        String title = (contentTitle == null || contentTitle.isBlank()) ? "복습 알림" : contentTitle;
        scheduleOffsetsFromDate(userId, contentId, title, base);
    }



    public void scheduleOffsetsFromDate(long userId, Long contentId, String contentTitle, LocalDate startDate, String imageUrl) {
        scheduleOffsetsFromDate(userId, contentId, contentTitle, startDate); // 이미지 미사용
    }


    public void scheduleOffsetsFromDate(long userId, Long contentId, String contentTitle, LocalDate startDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
        if (!user.isReceiveReminders()) return;

        if (contentId != null) {
            reminderRepository.deleteByUser_IdAndContentIdAndSentFalse(userId, contentId);
        }

        String title = (contentTitle == null || contentTitle.isBlank()) ? "복습 알림" : contentTitle;

        for (int d : OFFSETS) {
            LocalDateTime dueAt = LocalDateTime.of(startDate.plusDays(d), DEFAULT_SEND_TIME);
            // ★★★ CHANGED: Repository 시그니처에 맞게 imageUrl 제거
            reminderRepository.upsert(userId, contentId, title, d, dueAt);
        }
    }

    public void resetFromTomorrow(long userId, Long contentId, String contentTitle, String imageUrl) {
        LocalDate startDate = LocalDate.now(KST).plusDays(1);
        scheduleOffsetsFromDate(userId, contentId, contentTitle, startDate); // 이미지 미사용
    }


    public void resetFromTomorrow(long userId, Long contentId, String contentTitle) {
        LocalDate startDate = LocalDate.now(KST).plusDays(1);
        scheduleOffsetsFromDate(userId, contentId, contentTitle, startDate);
    }


    public void createDefaultReminders(long userId, String contentTitle, String imageUrl) {
        // no-op: 더 이상 기본 리마인더 만들지 않음
    }


    public void createDefaultReminders(long userId, String contentTitle) {
        // no-op: 더 이상 기본 리마인더 만들지 않음
    }


    @Transactional
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now(KST);
        List<Reminder> dueList = reminderRepository.findDue(now);

        for (Reminder r : dueList) {
            User u = r.getUser();

            // 수신 동의 꺼져 있으면 메일 없이 완료 처리
            if (!u.isReceiveReminders()) {
                int skip = reminderRepository.markSentIfPending(r.getId());
                if (skip == 1) {
                    log.info("[REMINDER] skipped(send off) id={}, contentId={}, offset={}",
                            r.getId(), r.getContentId(), r.getOffsetDays());
                }
                continue;
            }

            // 아직 안 보낸 건만 sent=true로
            int updated = reminderRepository.markSentIfPending(r.getId());
            if (updated == 1) {
                String title = (r.getContentTitle() == null || r.getContentTitle().isBlank())
                        ? "복습 알림" : r.getContentTitle();
                try {
                    emailService.sendReminder(u.getEmail(), title, r.getOffsetDays());
                    log.info("[REMINDER] sent id={}, contentId={}, offset={}, to={}",
                            r.getId(), r.getContentId(), r.getOffsetDays(), u.getEmail());
                } catch (Exception e) {
                    log.error("[REMINDER] send failed id={}, to={}, err={}", r.getId(), u.getEmail(), e.getMessage(), e);
                    // 필요 시 재시도 전략에 따라 sent=false로 되돌리는 로직 추가 가능
                }
            } else {
                log.debug("[REMINDER] already handled id={}, contentId={}, offset={}",
                        r.getId(), r.getContentId(), r.getOffsetDays());
            }
        }
    }

    //사용자가 알림 수신을 OFF로 전환 시
    public int cancelPendingReminders(long userId) {
        return reminderRepository.deleteAllPendingByUser(userId);
    }
}
