// spl.reborn.study.service.UserStudyService
package spl.reborn.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spl.reborn.notification.ReminderService;
import spl.reborn.study.entity.ReviewProgress;
import spl.reborn.study.entity.UserStudy;
import spl.reborn.study.repository.ReviewProgressRepository;
import spl.reborn.study.repository.UserStudyRepository;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserStudyService {

    private final UserStudyRepository userStudyRepository;
    private final UserRepository userRepository;                 // user 조회용
    private final ReminderService reminderService;
    private final ReviewProgressRepository reviewProgressRepository; // ★ 추가

    @Transactional
    public Long saveStudy(long userId, String contentTitle, LocalDate studyDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        UserStudy s = new UserStudy();
        s.setUser(user);                         // ★ 연관관계 세팅
        s.setContentTitle(contentTitle);
        s.setStudyDate(studyDate);
        userStudyRepository.save(s);

        // ★ 저장 직후 초기 복습 스케줄 시작(첫 복습일 = studyDate + 1일)
        createInitialReviewProgress(s);

        // 기존 리마인더 예약 (네가 쓰는 주기에 맞춰 유지)
        reminderService.createForStudy(userId, s.getId(), s.getContentTitle(), s.getStudyDate());

        return s.getId();
    }

    // ===== 내부 헬퍼 =====
    private void createInitialReviewProgress(UserStudy saved) {
        ReviewProgress rp = new ReviewProgress();
        rp.setUserStudy(saved);
        rp.setOwner(saved.getUser());
        rp.setStageIndex(0);                                 // 0 = 첫 단계
        rp.setCompleted(false);
        rp.setLastResult(null);
        rp.setNextReviewDate(saved.getStudyDate().plusDays(1)); // 첫 복습일

        reviewProgressRepository.save(rp);
    }
}
