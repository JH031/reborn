package spl.reborn.notification;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    // 지금 보낼 알림(아직 안 보냈고, 예정시각이 지남)
    @Query("select r from Reminder r where r.sent = false and r.dueAt <= :now")
    List<Reminder> findDue(@Param("now") LocalDateTime now);

    // 같은 콘텐츠의 미발송 예약을 지워 중복 방지 (콘텐츠 저장/수정 시)
    int deleteByUser_IdAndContentIdAndSentFalse(long userId, Long contentId);

    // 유저가 알림 OFF할 때 남은 예약 일괄 삭제
    @Modifying
    @Query("delete from Reminder r where r.user.id = :userId and r.sent = false")
    int deleteAllPendingByUser(@Param("userId") long userId);

    // ★ 경합 방지: 아직 안 보낸 건만 sent=true로 바꾸고 1을 반환. 이미 처리됐으면 0.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Reminder r set r.sent = true where r.id = :id and r.sent = false")
    int markSentIfPending(@Param("id") Long id);
}
