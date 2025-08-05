package spl.reborn.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spl.reborn.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}