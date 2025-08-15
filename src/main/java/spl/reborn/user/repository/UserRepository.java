package spl.reborn.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spl.reborn.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserid(String userid);
    boolean existsByUserid(String userid);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByNameIgnoreCaseAndEmailIgnoreCase(String name, String email);
    Optional<User> findByUseridAndEmailIgnoreCase(String userid, String email);

}
