// spl.reborn.study.repository.StudyCheckRepository
package spl.reborn.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spl.reborn.study.entity.ReviewProgress;
import spl.reborn.study.entity.StudyCheck;

import java.util.List;
import java.util.Optional;

public interface StudyCheckRepository extends JpaRepository<StudyCheck, Long> {

    //특정 유저 학습의 체크 이력
    List<StudyCheck> findByUser_IdAndUserStudy_IdOrderByStageDayAsc(long userId, long userStudyId);


    boolean existsByUser_IdAndUserStudy_IdAndStageDayAndResult(
            long userId, long userStudyId, int stageDay, StudyCheck.Result result);


    int deleteByUser_IdAndUserStudy_Id(long userId, long userStudyId);


    Optional<StudyCheck> findByUser_IdAndUserStudy_IdAndStageDay(
            long userId, long userStudyId, int stageDay
    );

}
