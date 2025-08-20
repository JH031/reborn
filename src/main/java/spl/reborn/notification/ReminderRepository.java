package spl.reborn.notification;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    // 오늘 마이페이지 카드용 프로젝션

    interface ReminderCardView {
        Long getId();
        Long getContentId();
        String getContentTitle();
        Integer getOffsetDays();
        LocalDateTime getDueAt();
        String getImageUrl(); // UserStudy.imageUrl
    }

    // ★ dueAt이 지났고 아직 안 보낸 건들
    @Query("""
        select r from Reminder r
        join fetch r.user u
        where r.sent = false and r.dueAt <= :now
        order by r.dueAt
    """)
    List<Reminder> findDue(@Param("now") LocalDateTime now);

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

    // 이미 sent=false일 때만 sent=true로
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Reminder r set r.sent = true where r.id = :id and r.sent = false")
    int markSentIfPending(@Param("id") Long id);

    // 중복 방지용
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Reminder r where r.user.id = :userId and r.contentId = :contentId and r.sent = false")
    int deleteByUser_IdAndContentIdAndSentFalse(@Param("userId") long userId, @Param("contentId") Long contentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Reminder r where r.user.id = :userId and r.sent = false")
    int deleteAllPendingByUser(@Param("userId") long userId);


    // sent 여부와 상관없이, 오늘 범위에 해당하는 문제, 이미지 함께
    @Query("""
        SELECT r.id           AS id,
               r.contentId    AS contentId,
               r.contentTitle AS contentTitle,
               r.offsetDays   AS offsetDays,
               r.dueAt        AS dueAt,
               us.imageUrl    AS imageUrl
        FROM Reminder r
        LEFT JOIN UserStudy us ON r.contentId = us.id
        WHERE r.user.id = :userId
          AND r.dueAt >= :start
          AND r.dueAt <  :end
        ORDER BY r.dueAt ASC
    """)
    List<ReminderCardView> findTodayCardsWithImageIncludingSent(@Param("userId") long userId,
                                                                @Param("start") LocalDateTime start,
                                                                @Param("end") LocalDateTime end);
}
