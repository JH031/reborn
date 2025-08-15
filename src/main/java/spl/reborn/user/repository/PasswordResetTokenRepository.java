package spl.reborn.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spl.reborn.user.entity.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
}
