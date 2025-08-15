package spl.reborn.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spl.reborn.study.entity.ReviewProgress;

import java.time.LocalDate;
import java.util.List;

public interface ReviewProgressRepository extends JpaRepository<ReviewProgress, Long> {

    // “해당 날짜에 해야 할 것만”
    @Query("""
        select rp from ReviewProgress rp
        join fetch rp.userStudy us
        join fetch rp.owner o
        where o.id = :userId
          and rp.completed = false
          and rp.nextReviewDate = :targetDate
    """)
    List<ReviewProgress> findDueOn(@Param("userId") Long userId,
                                   @Param("targetDate") LocalDate targetDate);

    // “기한 지난 것 + 오늘 것”
    @Query("""
        select rp from ReviewProgress rp
        join fetch rp.userStudy us
        join fetch rp.owner o
        where o.id = :userId
          and rp.completed = false
          and rp.nextReviewDate <= :targetDate
        order by rp.nextReviewDate asc
    """)
    List<ReviewProgress> findOverdueAndToday(@Param("userId") Long userId,
                                             @Param("targetDate") LocalDate targetDate);
}
