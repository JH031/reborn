// spl.reborn.study.controller.UserStudyController
package spl.reborn.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import spl.reborn.study.entity.StudyCheck;
import spl.reborn.study.service.StudyCheckService;
import spl.reborn.study.service.UserStudyService;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies")
public class UserStudyController {

    private final UserStudyService userStudyService;
    private final StudyCheckService studyCheckService;
    private final UserRepository userRepository; // ← 토큰의 userid(또는 username)로 DB에서 id를 찾기 위해 주입

    // 학습 기록 생성
    @PostMapping
    public ResponseEntity<Long> save(@RequestParam String contentTitle,
                                     @RequestParam LocalDate studyDate) {
        long currentUserId = getCurrentUserId();
        Long id = userStudyService.saveStudy(currentUserId, contentTitle, studyDate);
        return ResponseEntity.ok(id);
    }

    // 이해도 체크
    @PostMapping("/{studyId}/review")
    @Operation(
            summary = "이해도 체크",
            description = "지정된 복습 주기(1/4/7/14/30일)에서 UNDERSTOOD 또는 NOT_UNDERSTOOD 결과를 기록합니다."
    )
    public ResponseEntity<Void> review(@PathVariable long studyId,
                                       @RequestParam int stageDay,                 // 1/4/7/14/30
                                       @RequestParam StudyCheck.Result result) {   // UNDERSTOOD or NOT_UNDERSTOOD
        long currentUserId = getCurrentUserId();
        studyCheckService.check(currentUserId, studyId, stageDay, result);
        return ResponseEntity.ok().build();
    }

    // 체크 현황 조회
    @GetMapping("/{studyId}/review")
    public ResponseEntity<List<StudyCheck>> getReview(@PathVariable long studyId) {
        long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(studyCheckService.getChecks(currentUserId, studyId));
    }


    private long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Unauthenticated");
        }
        String useridOrUsername = auth.getName(); // JwtAuthenticationFilter에서 set한 사용자명
        User user = userRepository.findByUserid(useridOrUsername)  // ★ 너의 도메인에 맞추어 findByUserid 또는 findByUsername
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + useridOrUsername));
        return user.getId();
    }
}
