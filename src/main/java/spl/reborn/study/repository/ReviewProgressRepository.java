// spl.reborn.study.repository.ReviewProgressRepository.java
package spl.reborn.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spl.reborn.study.entity.ReviewProgress;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReviewProgressRepository extends JpaRepository<ReviewProgress, Long> {

    // 지정한 날짜에 해야 할 복습 목록

    @Query("""
        select rp
        from ReviewProgress rp
        join fetch rp.userStudy us
        where rp.owner.id = :userId
          and rp.completed = false
          and rp.nextReviewDate = :targetDate
        order by rp.nextReviewDate asc
    """)
    List<ReviewProgress> findDueOn(@Param("userId") Long userId,
                                   @Param("targetDate") LocalDate targetDate);


    @Query("""
        select rp
        from ReviewProgress rp
        join fetch rp.userStudy us
        where rp.owner.id = :userId
          and rp.completed = false
          and rp.nextReviewDate <= :targetDate
        order by rp.nextReviewDate asc
    """)
    List<ReviewProgress> findOverdueAndToday(@Param("userId") Long userId,
                                             @Param("targetDate") LocalDate targetDate);


    Optional<ReviewProgress> findByUserStudy_IdAndOwner_Id(Long userStudyId, Long ownerId);
}
