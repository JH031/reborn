package spl.reborn.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 토큰 주인
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 이메일로 보낼 난수 토큰
    @Column(nullable = false, unique = true, length = 120)
    private String token;

    // 만료 시간 (예: 발급 후 30분)
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 사용 여부(한 번만 사용)
    @Column(nullable = false)
    private boolean used = false;
}
