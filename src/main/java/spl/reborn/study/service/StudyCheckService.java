// spl.reborn.study.service.StudyCheckService
package spl.reborn.study.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spl.reborn.notification.ReminderRepository;
import spl.reborn.notification.ReminderService;
import spl.reborn.study.entity.ReviewProgress;
import spl.reborn.study.entity.StudyCheck;
import spl.reborn.study.entity.UserStudy;
import spl.reborn.study.repository.ReviewProgressRepository;
import spl.reborn.study.repository.StudyCheckRepository;
import spl.reborn.study.repository.UserStudyRepository;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyCheckService {

    /** 복습 오프셋(일) – stageIndex 0..n 과 매핑 */
    private static final int[] OFFSETS = {1, 4, 7, 14, 30};
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final UserStudyRepository userStudyRepository;
    private final StudyCheckRepository studyCheckRepository;

    private final ReviewProgressRepository reviewProgressRepository;

    private final ReminderRepository reminderRepository;
    private final ReminderService reminderService;

    /**
     * @param stageDay OFFSETS 중 하나여야 함 (1/4/7/14/30)
     */
    @Transactional
    public void check(long userId, long userStudyId, int stageDay, StudyCheck.Result result) {
        if (Arrays.stream(OFFSETS).noneMatch(d -> d == stageDay)) {
            throw new IllegalArgumentException("stageDay must be one of " + Arrays.toString(OFFSETS));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        UserStudy study = userStudyRepository.findById(userStudyId)
                .orElseThrow(() -> new IllegalArgumentException("study not found: " + userStudyId));

        ReviewProgress rp = reviewProgressRepository
                .findByUserStudy_IdAndOwner_Id(userStudyId, userId)
                .orElseThrow(() -> new IllegalArgumentException("review_progress not found for study=" + userStudyId + ", user=" + userId));

        LocalDate today = LocalDate.now(KST);

        log.info("[StudyCheck] BEFORE  studyId={}, userId={}, stageIndex={}, next={}, today={}, inputStageDay={}, result={}",
                userStudyId, userId, rp.getStageIndex(), rp.getNextReviewDate(), today, stageDay, result);

        // 1) 체크 기록 UPSERT (user + study + stageDay 유니크)
        upsertStudyCheck(user, study, stageDay, result);

        // 2) B정책: 당일/이후(today >= next_review_date)일 때만 진행도 갱신
        boolean due = !today.isBefore(rp.getNextReviewDate()); // today >= next

        if (due) {
            if (result == StudyCheck.Result.UNDERSTOOD) {
                // 현재 stageIndex + 1로 진급
                int nextStageIndex = rp.getStageIndex() + 1;
                if (nextStageIndex >= OFFSETS.length) {
                    // 마지막 단계 통과 시 완료 처리 (원하면 completed=true 등)
                    rp.setCompleted(true);
                    rp.setLastResult(ReviewProgress.UnderstandingResult.UNDERSTOOD);
                    // 완료된 경우 nextReviewDate는 유지하거나 null 처리 – 정책에 맞게 선택
                    // 여기서는 유지
                } else {
                    rp.setStageIndex(nextStageIndex);
                    rp.setLastResult(ReviewProgress.UnderstandingResult.UNDERSTOOD);
                    rp.setNextReviewDate(today.plusDays(OFFSETS[nextStageIndex]));
                }
                reviewProgressRepository.saveAndFlush(rp);
            } else {
                // NOT_UNDERSTOOD → 진행도 리셋(0단계로) + next = 오늘 + OFFSETS[0]
                rp.setStageIndex(0);
                rp.setCompleted(false);
                rp.setLastResult(ReviewProgress.UnderstandingResult.NOT_UNDERSTOOD);
                rp.setNextReviewDate(today.plusDays(OFFSETS[0]));
                reviewProgressRepository.saveAndFlush(rp);

                // 같은 학습의 기존 이해도 기록 전체 리셋(정책 유지)
                studyCheckRepository.deleteByUser_IdAndUserStudy_Id(userId, userStudyId);

                // 아직 안 보낸 리마인더 제거(중복 방지)
                reminderRepository.deleteByUser_IdAndContentIdAndSentFalse(userId, userStudyId);

                // 오늘을 기준으로 다시 예약
                reminderService.scheduleOffsetsFromDate(
                        userId,
                        study.getId(),
                        study.getContentTitle(),
                        today,
                        study.getImageUrl()
                );
            }
        } else {
            // 기한 전 → 진행도는 그대로, 로그만 남김
            log.info("[StudyCheck] NOT DUE YET: progress not updated (today={}, next={})", today, rp.getNextReviewDate());
        }

        log.info("[StudyCheck] AFTER   studyId={}, userId={}, stageIndex={}, next={}, completed={}",
                userStudyId, userId, rp.getStageIndex(), rp.getNextReviewDate(), rp.isCompleted());
    }

    @Transactional(readOnly = true)
    public List<StudyCheck> getChecks(long userId, long userStudyId) {
        return studyCheckRepository.findByUser_IdAndUserStudy_IdOrderByStageDayAsc(userId, userStudyId);
    }

    /** 중복키 방지: (user, study, stageDay) 있으면 UPDATE, 없으면 INSERT */
    private void upsertStudyCheck(User user, UserStudy study, int stageDay, StudyCheck.Result result) {
        Optional<StudyCheck> existing =
                studyCheckRepository.findByUser_IdAndUserStudy_IdAndStageDay(user.getId(), study.getId(), stageDay);

        if (existing.isPresent()) {
            StudyCheck sc = existing.get();
            sc.setResult(result);
            sc.setCheckedAt(LocalDateTime.now(KST));
            studyCheckRepository.save(sc); // UPDATE
        } else {
            StudyCheck sc = new StudyCheck();
            sc.setUser(user);
            sc.setUserStudy(study);
            sc.setStageDay(stageDay);
            sc.setResult(result);
            sc.setCheckedAt(LocalDateTime.now(KST));
            studyCheckRepository.save(sc); // INSERT
        }
    }
}
