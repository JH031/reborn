// spl.reborn.study.service.ReviewService.java
package spl.reborn.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spl.reborn.study.dto.ReviewDtos.DueReviewDto;
import spl.reborn.study.dto.ReviewDtos.UnderstandingReq;
import spl.reborn.study.entity.ReviewProgress;
import spl.reborn.study.repository.ReviewProgressRepository;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewProgressRepository reviewRepo;
    private final UserRepository userRepo;

    // 복습 주기(필요 시 1,3,7,30 등으로 변경 가능)
    private static final int[] SCHEDULE = {1, 4, 7, 14, 30};
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 현재 로그인한 사용자의 DB PK(id) 조회 */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userid = auth.getName(); // JWT subject = userid 라고 가정
        User u = userRepo.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("인증 사용자 조회 실패: " + userid));
        return u.getId();
    }

    // 해당 날짜에 해야 할 복습 목록
    public List<DueReviewDto> getDueList(LocalDate targetDate, boolean includeOverdue) {
        Long userId = getCurrentUserId();
        List<ReviewProgress> list = includeOverdue
                ? reviewRepo.findOverdueAndToday(userId, targetDate)
                : reviewRepo.findDueOn(userId, targetDate);
        return list.stream().map(DueReviewDto::from).toList();
    }

    // 이해도 기록
    @Transactional
    public void recordUnderstanding(Long reviewProgressId, UnderstandingReq req) {
        Long userId = getCurrentUserId();

        ReviewProgress rp = reviewRepo.findById(reviewProgressId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 복습 항목: " + reviewProgressId));

        // 본인 항목만 수정 가능
        if (rp.getOwner().getId() != userId.longValue()) {
            throw new SecurityException("본인 복습 항목만 수정할 수 있습니다.");
        }
        LocalDate today = LocalDate.now(KST);

        if (req.result == ReviewProgress.UnderstandingResult.UNDERSTOOD) {
            // 마지막 단계면 완료 처리
            if (rp.getStageIndex() >= SCHEDULE.length - 1) {
                rp.setCompleted(true);
            } else {
                rp.setStageIndex(rp.getStageIndex() + 1);
                int gap = SCHEDULE[rp.getStageIndex()];
                rp.setNextReviewDate(today.plusDays(gap));
            }
        } else { // NOT_UNDERSTOOD → 0단계로 리셋
            rp.setStageIndex(0);
            rp.setNextReviewDate(today.plusDays(SCHEDULE[0]));
        }
        rp.setLastResult(req.result);
    }
}
