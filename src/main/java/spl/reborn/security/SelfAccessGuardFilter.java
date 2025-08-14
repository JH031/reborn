package spl.reborn.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
        String method = request.getMethod();

        // 0) 아직 인증 전(permitAll 포함)이면 이 필터에서 막지 않음
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }

        // 1) 관리자면 항상 통과
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (isAdmin) {
            chain.doFilter(request, response);
            return;
        }

        // 2) 리뷰 엔드포인트는 가드 스킵(선택)
        //   POST /api/studies/{id}/review
        if ("POST".equalsIgnoreCase(method) && uri.matches("^/api/studies/\\d+/review$")) {
            chain.doFilter(request, response);
            return;
        }

        // 3) JWT 기반 컨트롤러는 userId 파라미터가 필요 없음.
        //    -> 파라미터가 "있는 경우에만" 자기자신 검사를 수행.
        String userIdParam = request.getParameter("userId");
        if (userIdParam == null || userIdParam.isBlank()) {
            // 파라미터가 없으면 검사하지 않음(컨트롤러가 JWT로 현재 사용자 사용)
            chain.doFilter(request, response);
            return;
        }

        // 4) 파라미터가 있으면 자기자신인지 확인
        long requestedUserId;
        try {
            requestedUserId = Long.parseLong(userIdParam);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "userId 형식이 올바르지 않습니다.");
            return;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증 정보가 올바르지 않습니다.");
            return;
        }

        // 토큰의 username(userid)로 DB에서 내 PK(id) 조회
        String userid = userDetails.getUsername();
        long myId = userRepository.findByUserid(userid)
                .map(User::getId)
                .orElse(-1L);

        if (requestedUserId != myId) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "다른 사용자로는 요청할 수 없습니다.");
            return;
        }

        chain.doFilter(request, response);
    }
}
