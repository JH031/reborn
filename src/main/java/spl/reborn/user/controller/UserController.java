package spl.reborn.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import spl.reborn.user.dto.LoginRequest;
import spl.reborn.user.dto.SignUpRequest;
import spl.reborn.user.dto.TokenResponse;
import spl.reborn.user.entity.User;
import spl.reborn.user.service.UserService;
import spl.reborn.security.JwtUtil;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignUpRequest request) {
        userService.registerUser(request);
        return ResponseEntity.ok("회원가입 완료");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userService.findByUserid(request.getUserid());


        System.out.println("입력된 비밀번호: " + request.getPassword());
        System.out.println("DB 저장된 비밀번호: " + user.getPassword());


        if (!request.getPassword().equals(user.getPassword())) {
            return ResponseEntity.status(401).body("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getName());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}

