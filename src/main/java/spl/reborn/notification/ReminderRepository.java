package spl.reborn.notification;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    // 오늘 마이페이지 카드용 프로젝션 (엔티티 저장 아님: 조회 전용)
    // 파일을 나누기 번거로우면 이렇게 내부 인터페이스로 두셔도 됩니다.
    interface ReminderCardView {
        Long getId();
        Long getContentId();
        String getContentTitle();
        Integer getOffsetDays();
        LocalDateTime getDueAt();
        String getImageUrl(); // UserStudy.imageUrl
    }

    // ★ dueAt이 지났고 아직 안 보낸 건들 (user fetch join으로 LAZY 문제 차단)
    @Query("""
        select r from Reminder r
        join fetch r.user u
        where r.sent = false and r.dueAt <= :now
        order by r.dueAt
    """)
    List<Reminder> findDue(@Param("now") LocalDateTime now);

    // ★ 중복이면 덮어쓰기(업서트). 유니크키 (user_id, content_id, offset_days) 가정
    //    imageUrl은 저장하지 않으므로 파라미터/쿼리에서 제거
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

    // ====================== 마이페이지용 조회 (이메일 발송 여부와 무관) ======================

    // sent 여부와 상관없이, 오늘(KST 기준) due 범위에 해당하는 카드 + UserStudy 이미지 함께 조회
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
