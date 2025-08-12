package spl.reborn.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SelfAccessGuardFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        boolean isStudiesWrite = "POST".equalsIgnoreCase(request.getMethod())
                && uri.startsWith("/api/studies");

        if (isStudiesWrite) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof UserDetails userDetails) {

                boolean isAdmin = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_ADMIN"::equals);
                if (!isAdmin) {
                    String userIdParam = request.getParameter("userId");
                    if (userIdParam == null || userIdParam.isBlank()) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "userId가 필요합니다.");
                        return;
                    }
                    long requestedUserId;
                    try {
                        requestedUserId = Long.parseLong(userIdParam);
                    } catch (NumberFormatException e) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "userId 형식이 올바르지 않습니다.");
                        return;
                    }

                    // 토큰의 username == userid 로 DB에서 내 id 조회
                    String userid = userDetails.getUsername();
                    long myId = userRepository.findByUserid(userid)
                            .map(User::getId)
                            .orElse(-1L);

                    if (requestedUserId != myId) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "다른 사용자로는 요청할 수 없습니다.");
                        return;
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }
}
