package spl.reborn.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spl.reborn.security.JwtUtil;
import spl.reborn.user.dto.*;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository; // ★ 추가
import spl.reborn.user.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository; // ★ 추가

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
                user.getEmail()
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
