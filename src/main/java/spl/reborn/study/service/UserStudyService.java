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


    @Transactional
    public Long saveStudy(long userId, String contentTitle, LocalDate studyDate) {
        // ★★★ ADDED: 오버로드로 위임
        return saveStudy(userId, contentTitle, studyDate, null);
    }

    @Transactional
    public Long saveStudy(long userId, String contentTitle, LocalDate studyDate, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        UserStudy s = new UserStudy();
        s.setUser(user);
        s.setContentTitle(contentTitle);
        s.setStudyDate(studyDate);
        s.setImageUrl(imageUrl);
        userStudyRepository.save(s);


        createInitialReviewProgress(s);


        reminderService.createForStudy(
                userId,
                s.getId(),
                s.getContentTitle(),
                s.getStudyDate()
        );

        return s.getId();
    }


    private void createInitialReviewProgress(UserStudy saved) {
        ReviewProgress rp = new ReviewProgress();
        rp.setUserStudy(saved);
        rp.setOwner(saved.getUser());
        rp.setStageIndex(0);
        rp.setCompleted(false);
        rp.setLastResult(null);
        rp.setNextReviewDate(saved.getStudyDate().plusDays(1));
        rp.setImageUrl(saved.getImageUrl());
        reviewProgressRepository.save(rp);
    }
}
