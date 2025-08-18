// spl.reborn.study.dto.ReviewDtos.java
package spl.reborn.study.dto;

import spl.reborn.study.entity.ReviewProgress;
// 리마인더 조회 프로젝션으로부터도 DTO를 만들 수 있도록 import
import spl.reborn.notification.ReminderRepository.ReminderCardView;

import java.time.LocalDate;

public class ReviewDtos {

    /** (응답) 특정 날짜에 해야 할 복습 목록 아이템 */
    public record DueReviewDto(
            Long reviewProgressId,
            Long userStudyId,
            String contentTitle,
            int stageIndex,
            LocalDate nextReviewDate,
            String imageUrl          // 이미지 URL (원본/대표)
    ) {
        /** ReviewProgress 기반 매핑
         *  ★★★ CHANGED: 이미지 경로를 UserStudy가 아닌 ReviewProgress에서 직접 조회
         */
        public static DueReviewDto from(ReviewProgress rp) {
            return new DueReviewDto(
                    rp.getId(),
                    rp.getUserStudy().getId(),
                    rp.getUserStudy().getContentTitle(),
                    rp.getStageIndex(),
                    rp.getNextReviewDate(),
                    rp.getImageUrl() // <-- CHANGED
            );
        }

        /** ReminderRepository.ReminderCardView 기반 매핑 (리마인더 조인 결과 사용) */
        public static DueReviewDto from(ReminderCardView v) {
            return new DueReviewDto(
                    null,                               // reviewProgressId: 리마인더 조회에는 없음
                    v.getContentId(),                   // userStudyId = contentId
                    v.getContentTitle(),
                    -1,                                 // stageIndex 정보 없음 → -1
                    (v.getDueAt() != null ? v.getDueAt().toLocalDate() : null), // dueAt → LocalDate
                    v.getImageUrl()                     // UserStudy.imageUrl (LEFT JOIN 결과)
            );
        }
    }

    /** (요청) 이해도 기록 */
    public static class UnderstandingReq {
        public ReviewProgress.UnderstandingResult result;
    }
}
