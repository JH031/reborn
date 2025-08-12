package spl.reborn.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import spl.reborn.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /** username == userid */
    @Override
    public UserDetails loadUserByUsername(String userid) throws UsernameNotFoundException {
        spl.reborn.user.entity.User u = userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userid));

        return new User(
                u.getUserid(),                // username
                u.getPassword(),              // encoded password
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
