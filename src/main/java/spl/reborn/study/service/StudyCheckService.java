// spl.reborn.study.service.StudyCheckService
package spl.reborn.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spl.reborn.notification.ReminderRepository;
import spl.reborn.notification.ReminderService;
import spl.reborn.study.entity.StudyCheck;
import spl.reborn.study.entity.UserStudy;
import spl.reborn.study.repository.StudyCheckRepository;
import spl.reborn.study.repository.UserStudyRepository;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyCheckService {

    private static final int[] OFFSETS = {1, 4, 7, 14, 30};
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final UserStudyRepository userStudyRepository;
    private final StudyCheckRepository studyCheckRepository;
    private final ReminderRepository reminderRepository;
    private final ReminderService reminderService;

    @Transactional
    public void check(long userId, long userStudyId, int stageDay, StudyCheck.Result result) {
        if (Arrays.stream(OFFSETS).noneMatch(d -> d == stageDay)) {
            throw new IllegalArgumentException("stageDay must be one of " + Arrays.toString(OFFSETS));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        UserStudy study = userStudyRepository.findById(userStudyId)
                .orElseThrow(() -> new IllegalArgumentException("study not found: " + userStudyId));

        // 1) 이해도 체크 저장 (user+study+stageDay 유니크 권장)
        StudyCheck sc = new StudyCheck();
        sc.setUser(user);
        sc.setUserStudy(study);
        sc.setStageDay(stageDay);
        sc.setResult(result);
        studyCheckRepository.save(sc);

        // 2) 결과별 처리
        if (result == StudyCheck.Result.NOT_UNDERSTOOD) {
            // (a) 같은 학습의 기존 이해도 기록 리셋
            studyCheckRepository.deleteByUser_IdAndUserStudy_Id(userId, userStudyId);

            // (b) 해당 학습의 아직 안 보낸 리마인더 정리(중복 방지)
            reminderRepository.deleteByUser_IdAndContentIdAndSentFalse(userId, userStudyId);

            // (c) 오늘을 시작일로 1/4/7/14/30 재예약 (ReminderService 내부는 업서트 사용)
            LocalDate startDate = LocalDate.now(KST);
            reminderService.scheduleOffsetsFromDate(
                    userId,
                    study.getId(),
                    study.getContentTitle(),
                    startDate
            );
        }
        // UNDERSTOOD: 재예약 없음(체크만 누적)
    }

    @Transactional(readOnly = true)
    public List<StudyCheck> getChecks(long userId, long userStudyId) {
        return studyCheckRepository.findByUser_IdAndUserStudy_IdOrderByStageDayAsc(userId, userStudyId);
    }
}
