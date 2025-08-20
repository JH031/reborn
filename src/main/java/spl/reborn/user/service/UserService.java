package spl.reborn.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spl.reborn.notification.ReminderService;
import spl.reborn.user.dto.SignUpRequest;
import spl.reborn.user.dto.UpdateUserRequest;
import spl.reborn.user.dto.UserProfileResponse;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReminderService reminderService;

    @Transactional
    public void registerUser(SignUpRequest request) {
        // 아이디 중복 체크
        if (userRepository.existsByUserid(request.getUserid())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 3) 사용자 생성
        User user = new User();
        user.setName(request.getName());
        user.setUserid(request.getUserid());
        user.setPassword(request.getPassword()); // 암호화 없이 그대로 저장
        user.setEmail(request.getEmail());
        user.setGrade(request.getGrade());
        user.setSchool(request.getSchool());
        user.setReceiveReminders(request.isReceiveReminders());

        userRepository.save(user);

        //
        if (user.isReceiveReminders()) {
            reminderService.createDefaultReminders(user.getId(), "복습 알림", null);
        }
    }

    @Transactional(readOnly = true)
    public User findByUserid(String userid) {
        return userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userid));
    }

    @Transactional
    public UserProfileResponse updateUserProfile(long userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String newEmail = req.getEmail().trim();
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmailAndIdNot(newEmail, userId)) {
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }
                user.setEmail(newEmail);
            }
        }

        if (req.getGrade() != null) {
            int g = req.getGrade();
            user.setGrade(g);
        }

        if (req.getSchool() != null) {
            user.setSchool(req.getSchool());
        }

        userRepository.save(user);

        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .userid(user.getUserid())
                .email(user.getEmail())
                .grade(user.getGrade())
                .school(user.getSchool())
                .receiveReminders(user.isReceiveReminders())
                .build();
    }

    //아이디 찾기
    @Transactional(readOnly = true)
    public String findUseridByNameAndEmail(String name, String email) {
        String trimmedName = name == null ? null : name.trim();
        String trimmedEmail = email == null ? null : email.trim();

        return userRepository.findByNameIgnoreCaseAndEmailIgnoreCase(trimmedName, trimmedEmail)
                .map(User::getUserid)
                .orElseThrow(() -> new IllegalArgumentException("해당 이름/이메일로 가입된 아이디가 없습니다."));
    }
}
