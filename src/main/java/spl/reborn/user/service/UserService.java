package spl.reborn.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spl.reborn.notification.ReminderService;
import spl.reborn.user.dto.SignUpRequest;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReminderService reminderService;

    @Transactional
    public void registerUser(SignUpRequest request) {
        // 1) 아이디 중복 체크
        if (userRepository.existsByUserid(request.getUserid())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 2) 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 3) 사용자 생성 (비밀번호 암호화 X)
        User user = new User();
        user.setName(request.getName());
        user.setUserid(request.getUserid());
        user.setPassword(request.getPassword()); // 암호화 없이 그대로 저장
        user.setEmail(request.getEmail());
        user.setGrade(request.getGrade());
        user.setSchool(request.getSchool());
        user.setReceiveReminders(request.isReceiveReminders());

        userRepository.save(user);

        // 4) 리마인더 동의 시 기본 스케줄 생성
        if (user.isReceiveReminders()) {
            reminderService.createDefaultReminders(user.getId(), null);
        }
    }

    @Transactional(readOnly = true)
    public User findByUserid(String userid) {
        return userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userid));
    }
}
