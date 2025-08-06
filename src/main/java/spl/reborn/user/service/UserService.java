package spl.reborn.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import spl.reborn.user.dto.SignUpRequest;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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

        userRepository.save(user);
    }

    public User findByUserid(String userid) {
        return userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("해당 아이디 사용자가 존재하지 않습니다: " + userid));
    }
}
