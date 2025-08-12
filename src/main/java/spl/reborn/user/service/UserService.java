package spl.reborn.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spl.reborn.user.dto.SignUpRequest;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;
import spl.reborn.notification.ReminderService; // ★ 추가

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReminderService reminderService; // ★ 추가

    @Transactional
    public void registerUser(SignUpRequest request) {
        if (userRepository.findByUserid(request.getUserid()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        User user = new User();
        user.setName(request.getName());
        user.setUserid(request.getUserid());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setGrade(request.getGrade());
        user.setSchool(request.getSchool());
        user.setReceiveReminders(request.isReceiveReminders()); // ★ 동의 저장

        userRepository.save(user);

        if (user.isReceiveReminders()) {
            reminderService.createDefaultReminders(user.getId(), null); // ★ 딱 이 한 줄
        }
    }

    public User findByUserid(String userid) {
        return userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("사용가능한 아이디입니다: " + userid));
    }
}
