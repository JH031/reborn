// spl.reborn.study.service.UserStudyService
package spl.reborn.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spl.reborn.notification.ReminderService;
import spl.reborn.study.entity.UserStudy;
import spl.reborn.study.repository.UserStudyRepository;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserStudyService {

    private final UserStudyRepository userStudyRepository;
    private final UserRepository userRepository;           // user 조회용
    private final ReminderService reminderService;

    @Transactional
    public Long saveStudy(long userId, String contentTitle, LocalDate studyDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        UserStudy s = new UserStudy();
        s.setUser(user);                                   // ★ 연관관계로 세팅
        s.setContentTitle(contentTitle);
        s.setStudyDate(studyDate);
        userStudyRepository.save(s);

        // ★ 저장된 콘텐츠마다 1/3/7/30일 예약 생성
        reminderService.createForStudy(userId, s.getId(), s.getContentTitle(), s.getStudyDate());

        return s.getId();
    }
}
