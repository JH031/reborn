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

    private static final int[] OFFSETS = {1, 4, 7, 14, 30}; // 0일 즉시 예약도 원하면 {0,1,4,7,14,30}
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime DEFAULT_SEND_TIME = LocalTime.of(9, 0);

    /**
     * 학습 콘텐츠 저장 시: 해당 콘텐츠마다 1/4/7/14/30일 예약 생성
     * (기준일: studyDate, null이면 오늘)
     *
     * 이미지 URL은 DB에 저장하지 않으므로 인자는 무시하고 오버로드로 위임합니다.
     */
    // ★★★ CHANGED: 이미지 인자는 받되 내부에서 사용하지 않음(호환용)
    public void createForStudy(long userId, Long contentId, String contentTitle, LocalDate studyDate, String imageUrl) {
        LocalDate base = (studyDate != null) ? studyDate : LocalDate.now(KST);
        String title = (contentTitle == null || contentTitle.isBlank()) ? "복습 알림" : contentTitle;
        scheduleOffsetsFromDate(userId, contentId, title, base); // 이미지 미사용
    }

    // ★★★ ADDED: 이미지 없는 정식 오버로드
    public void createForStudy(long userId, Long contentId, String contentTitle, LocalDate studyDate) {
        LocalDate base = (studyDate != null) ? studyDate : LocalDate.now(KST);
        String title = (contentTitle == null || contentTitle.isBlank()) ? "복습 알림" : contentTitle;
        scheduleOffsetsFromDate(userId, contentId, title, base);
    }

    /**
     * 임의의 시작일(startDate) 기준으로 1/4/7/14/30 예약 생성.
     * - 사용자 수신 동의 OFF면 아무 것도 하지 않음
     * - 같은 콘텐츠의 미발송 예약은 먼저 삭제(중복 방지)
     */
    // ★★★ CHANGED: 이미지 인자는 받되 내부에서 사용하지 않음(호환용)
    public void scheduleOffsetsFromDate(long userId, Long contentId, String contentTitle, LocalDate startDate, String imageUrl) {
        scheduleOffsetsFromDate(userId, contentId, contentTitle, startDate); // 이미지 미사용
    }

    // ★★★ ADDED: 이미지 없는 정식 구현
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

    /**
     * NOT_UNDERSTOOD 처리용: 내일부터 다시 1/4/7/14/30 재예약
     * - 같은 콘텐츠의 미발송 예약은 먼저 삭제
     */
    // ★★★ CHANGED: 이미지 인자는 받되 내부에서 사용하지 않음(호환용)
    public void resetFromTomorrow(long userId, Long contentId, String contentTitle, String imageUrl) {
        LocalDate startDate = LocalDate.now(KST).plusDays(1);
        scheduleOffsetsFromDate(userId, contentId, contentTitle, startDate); // 이미지 미사용
    }

    // ★★★ ADDED: 이미지 없는 정식 오버로드
    public void resetFromTomorrow(long userId, Long contentId, String contentTitle) {
        LocalDate startDate = LocalDate.now(KST).plusDays(1);
        scheduleOffsetsFromDate(userId, contentId, contentTitle, startDate);
    }

    /** 사용자 단위 기본 예약 생성(옵션) */
// 오버로드 호환용 - 이미지 인자 버전
    public void createDefaultReminders(long userId, String contentTitle, String imageUrl) {
        // no-op: 더 이상 기본 리마인더 만들지 않음
    }

    // 오버로드 호환용 - 이미지 없는 버전
    public void createDefaultReminders(long userId, String contentTitle) {
        // no-op: 더 이상 기본 리마인더 만들지 않음
    }

    /**
     * 지금 보낼 알림을 찾아 전송. 경합/중복 호출이어도 한 번만 나가도록 방어
     * - findDue(now): user fetch join으로 LAZY 문제 방지
     * - markSentIfPending: 선점(원자적 sent=true) 후 실제 발송
     */
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

            // 아직 안 보낸 건만 sent=true로 선점 → 성공한 스레드만 발송
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

    /** 사용자가 알림 수신을 OFF로 전환 시: 아직 안 보낸 예약 일괄 삭제 */
    public int cancelPendingReminders(long userId) {
        return reminderRepository.deleteAllPendingByUser(userId);
    }
}
