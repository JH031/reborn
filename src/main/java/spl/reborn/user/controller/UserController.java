package spl.reborn.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spl.reborn.security.JwtUtil;
import spl.reborn.user.dto.*;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;
import spl.reborn.user.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @GetMapping("/check-id")
    public ResponseEntity<Void> checkId(@RequestParam String userid) {
        boolean exists = userRepository.existsByUserid(userid.trim());
        return exists ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<Void> checkEmail(@RequestParam String email) {
        boolean exists = userRepository.existsByEmail(email.trim());
        return exists ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignUpRequest request) {
        userService.registerUser(request);
        return ResponseEntity.ok("회원가입 완료");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        User user = userService.findByUserid(request.getUserid());

        System.out.println("입력된 비밀번호: " + request.getPassword());
        System.out.println("DB 저장된 비밀번호: " + user.getPassword());

        if (!request.getPassword().equals(user.getPassword())) {
            return ResponseEntity.status(401).build();
        }

        String token = jwtUtil.generateToken(user.getUserid());

        TokenResponse resp = new TokenResponse(
                token,
                user.getId(),     // PK id
                user.getName(),
                user.getEmail(),
                user.getGrade(),
                user.getSchool()
        );
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/{userId}/profile")
    public UserProfileResponse updateProfile(
            @PathVariable long userId,
            @Valid @RequestBody UpdateUserRequest req
    ) {
        return userService.updateUserProfile(userId, req);
    }

    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@Valid @RequestBody FindIdRequest request) {
        String userid = userService.findUseridByNameAndEmail(request.getName(), request.getEmail());
        return ResponseEntity.ok(new FindIdResponse(userid));
    }
}
