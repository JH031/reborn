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

    /**
     * 학습 콘텐츠 저장 시: 해당 콘텐츠마다 1/4/7/14/30일 예약 생성
     * (기준일: studyDate, null이면 오늘)
     */
    public void createForStudy(long userId, Long contentId, String contentTitle, LocalDate studyDate) {
        LocalDate base = (studyDate != null) ? studyDate : LocalDate.now(KST);
        String title = (contentTitle == null || contentTitle.isBlank()) ? "복습 알림" : contentTitle;
        scheduleOffsetsFromDate(userId, contentId, title, base);
    }

    /**
     * ✅ 임의의 시작일(startDate) 기준으로 1/4/7/14/30 예약 생성.
     * - 사용자 수신 동의 OFF면 아무 것도 하지 않음
     * - 같은 콘텐츠의 미발송 예약은 먼저 삭제(중복 방지)
     */
    public void scheduleOffsetsFromDate(long userId, Long contentId, String contentTitle, LocalDate startDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
        if (!user.isReceiveReminders()) return;

        // 같은 콘텐츠의 미발송 예약 제거(중복 방지)
        if (contentId != null) {
            reminderRepository.deleteByUser_IdAndContentIdAndSentFalse(userId, contentId);
        }

        String title = (contentTitle == null || contentTitle.isBlank()) ? "복습 알림" : contentTitle;

        for (int d : OFFSETS) {
            LocalDate dueDate = startDate.plusDays(d);
            LocalDateTime dueAt = LocalDateTime.of(dueDate, DEFAULT_SEND_TIME);

            Reminder r = new Reminder();
            r.setUser(user);
            r.setContentId(contentId);
            r.setContentTitle(title);
            r.setOffsetDays(d);
            r.setDueAt(dueAt);
            r.setSent(false);
            reminderRepository.save(r);
        }
    }

    /**
     * ✅ NOT_UNDERSTOOD 처리용: 내일부터 다시 1/4/7/14/30 재예약
     * - 같은 콘텐츠의 미발송 예약은 먼저 삭제
     */
    public void resetFromTomorrow(long userId, Long contentId, String contentTitle) {
        LocalDate startDate = LocalDate.now(KST).plusDays(1);
        scheduleOffsetsFromDate(userId, contentId, contentTitle, startDate);
    }

    /** 사용자 단위 기본 예약 생성(옵션) */
    public void createDefaultReminders(long userId, String contentTitle) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
        if (!user.isReceiveReminders()) return;

        LocalDate base = LocalDate.now(KST);
        String title = (contentTitle == null || contentTitle.isBlank()) ? "복습 알림" : contentTitle;

        for (int d : OFFSETS) {
            Reminder r = new Reminder();
            r.setUser(user);
            r.setContentId(null);
            r.setContentTitle(title);
            r.setOffsetDays(d);
            r.setDueAt(LocalDateTime.of(base.plusDays(d), DEFAULT_SEND_TIME));
            r.setSent(false);
            reminderRepository.save(r);
        }
    }

    /**
     * 지금 보낼 알림을 찾아 전송. 경합/중복 호출이어도 한 번만 나가도록 방어
     *
     * ※ 중요: readOnly 트랜잭션으로 열어서 LAZY 연관(Reminder.user) 초기화 보장
     *   (레포에서 user를 fetch join하면 더 견고해짐)
     */
    @Transactional
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now(KST);

        // 권장: findDue(now)가 user를 fetch join 해서 가져오도록 구현
        List<Reminder> dueList = reminderRepository.findDue(now);

        for (Reminder r : dueList) {
            User u = r.getUser(); // 트랜잭션 안에서 안전하게 초기화됨

            // 수신 동의 꺼져 있으면 메일 없이 완료 처리
            if (!u.isReceiveReminders()) {
                int skip = reminderRepository.markSentIfPending(r.getId());
                if (skip == 1) {
                    log.info("[REMINDER] skipped(send off) id={}, contentId={}, offset={}",
                            r.getId(), r.getContentId(), r.getOffsetDays());
                }
                continue;
            }

            // ★ 아직 안 보낸 건만 선점(sent=true) -> 성공한 스레드만 실제 발송
            int updated = reminderRepository.markSentIfPending(r.getId());
            if (updated == 1) {
                String title = (r.getContentTitle() == null || r.getContentTitle().isBlank())
                        ? "복습 알림" : r.getContentTitle();
                try {
                    emailService.sendReminder(u.getEmail(), title, r.getOffsetDays());
                    log.info("[REMINDER] sent id={}, contentId={}, offset={}, to={}",
                            r.getId(), r.getContentId(), r.getOffsetDays(), u.getEmail());
                } catch (Exception e) {
                    // (선택) 실패 시 재시도 로직을 두고 싶다면 여기서 sent=false로 되돌리는 처리 추가 가능
                    log.error("[REMINDER] send failed id={}, to={}, err={}", r.getId(), u.getEmail(), e.getMessage(), e);
                }
            } else {
                // 누군가 이미 처리한 건
                log.debug("[REMINDER] already handled id={}, contentId={}, offset={}",
                        r.getId(), r.getContentId(), r.getOffsetDays());
            }
        }
    }

    /** 사용자가 알림 수신을 OFF로 전환 시: 아직 안 보낸 예약 일괄 삭제 */
    public int cancelPendingReminders(long userId) {
        return reminderRepository.deleteAllPendingByUser(userId);
    }
}
