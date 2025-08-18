// spl.reborn.study.repository.StudyCheckRepository
package spl.reborn.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spl.reborn.study.entity.ReviewProgress;
import spl.reborn.study.entity.StudyCheck;

import java.util.List;
import java.util.Optional;

public interface StudyCheckRepository extends JpaRepository<StudyCheck, Long> {

    /** 특정 유저/학습의 체크 이력(stageDay 오름차순) */
    List<StudyCheck> findByUser_IdAndUserStudy_IdOrderByStageDayAsc(long userId, long userStudyId);

    /** 동일 (user, study, stageDay, result) 조합 존재 여부 */
    boolean existsByUser_IdAndUserStudy_IdAndStageDayAndResult(
            long userId, long userStudyId, int stageDay, StudyCheck.Result result);

    /** 동일 (user, study) 전체 이력 삭제 (NOT_UNDERSTOOD 시 리셋용) */
    int deleteByUser_IdAndUserStudy_Id(long userId, long userStudyId);

    /** UPSERT용: 동일 (user, study, stageDay) 1건 조회 */
    Optional<StudyCheck> findByUser_IdAndUserStudy_IdAndStageDay(
            long userId, long userStudyId, int stageDay
    );

}
