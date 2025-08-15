// spl.reborn.study.controller.ReviewController.java
package spl.reborn.study.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spl.reborn.study.dto.ReviewDtos.DueReviewDto;
import spl.reborn.study.dto.ReviewDtos.UnderstandingReq;
import spl.reborn.study.service.ReviewService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 마이페이지 > 복습 목록 (오늘 또는 지정일) */
    @GetMapping("/due")
    public ResponseEntity<List<DueReviewDto>> getDue(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "false") boolean includeOverdue
    ) {
        LocalDate target = (date == null || date.isBlank())
                ? LocalDate.now(KST)
                : LocalDate.parse(date); // yyyy-MM-dd
        return ResponseEntity.ok(reviewService.getDueList(target, includeOverdue));
    }

    /** 이해도 기록 (UNDERSTOOD / NOT_UNDERSTOOD) */
    @PostMapping("/{reviewProgressId}/result")
    public ResponseEntity<Void> record(
            @PathVariable Long reviewProgressId,
            @RequestBody UnderstandingReq req
    ) {
        reviewService.recordUnderstanding(reviewProgressId, req);
        return ResponseEntity.ok().build();
    }
}
