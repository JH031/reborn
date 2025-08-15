package spl.reborn.notification;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    // ★ dueAt이 지났고 아직 안 보낸 건들 (user fetch join으로 LAZY 문제 차단)
    @Query("""
        select r from Reminder r
        join fetch r.user u
        where r.sent = false and r.dueAt <= :now
        order by r.dueAt
    """)
    List<Reminder> findDue(@Param("now") LocalDateTime now);

    // ★ 중복이면 덮어쓰기(업서트). 유니크키 (user_id, content_id, offset_days) 가정
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO reminder (user_id, content_id, content_title, offset_days, due_at, sent)
        VALUES (:userId, :contentId, :title, :offsetDays, :dueAt, false)
        ON DUPLICATE KEY UPDATE
            content_title = VALUES(content_title),
            due_at        = VALUES(due_at),
            sent          = false
        """, nativeQuery = true)
    int upsert(@Param("userId") long userId,
               @Param("contentId") Long contentId,
               @Param("title") String title,
               @Param("offsetDays") int offsetDays,
               @Param("dueAt") LocalDateTime dueAt);

    // ★ 경합 방지용 선점(이미 sent=false일 때만 sent=true로)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Reminder r set r.sent = true where r.id = :id and r.sent = false")
    int markSentIfPending(@Param("id") Long id);

    // 중복 방지용 사전 정리(옵션) : 특정 콘텐츠의 미발송 예약 제거
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Reminder r where r.user.id = :userId and r.contentId = :contentId and r.sent = false")
    int deleteByUser_IdAndContentIdAndSentFalse(@Param("userId") long userId, @Param("contentId") Long contentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Reminder r where r.user.id = :userId and r.sent = false")
    int deleteAllPendingByUser(@Param("userId") long userId);
}
