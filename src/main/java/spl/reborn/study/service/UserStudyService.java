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
    private final UserRepository userRepository;
    private final ReminderService reminderService;
    private final ReviewProgressRepository reviewProgressRepository;

    /**
     * 기존 시그니처 유지 (호환용)
     * 이미지 URL이 없다면 null 로 저장됩니다.
     */
    @Transactional
    public Long saveStudy(long userId, String contentTitle, LocalDate studyDate) {
        // ★★★ ADDED: 오버로드로 위임
        return saveStudy(userId, contentTitle, studyDate, null);
    }

    /**
     * ★★★ ADDED: 이미지 URL까지 함께 저장하는 오버로드
     * - UserStudy 저장
     * - ReviewProgress 초기 행 생성 (첫 복습일 = studyDate + 1)
     * - Reminder 예약 (이미지 저장은 하지 않음)
     */
    @Transactional
    public Long saveStudy(long userId, String contentTitle, LocalDate studyDate, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        UserStudy s = new UserStudy();
        s.setUser(user);                         // 연관관계
        s.setContentTitle(contentTitle);
        s.setStudyDate(studyDate);
        s.setImageUrl(imageUrl);                // ★★★ CHANGED: 이미지 저장
        userStudyRepository.save(s);

        // ★★★ ADDED: 초기 복습 상태 생성 (마이페이지용)
        createInitialReviewProgress(s);

        // ★★★ CHANGED: 리마인더는 이미지 없이 예약 (Repository 시그니처 맞춤)
        reminderService.createForStudy(
                userId,
                s.getId(),
                s.getContentTitle(),
                s.getStudyDate()
        );

        return s.getId();
    }

    // ===== 내부 헬퍼 =====
    private void createInitialReviewProgress(UserStudy saved) {
        ReviewProgress rp = new ReviewProgress();
        rp.setUserStudy(saved);
        rp.setOwner(saved.getUser());
        rp.setStageIndex(0);
        rp.setCompleted(false);
        rp.setLastResult(null);
        rp.setNextReviewDate(saved.getStudyDate().plusDays(1));
        rp.setImageUrl(saved.getImageUrl());  // ★ UserStudy에서 이미지 복사
        reviewProgressRepository.save(rp);
    }
}
