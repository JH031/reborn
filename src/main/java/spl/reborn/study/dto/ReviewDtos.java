// spl.reborn.study.dto.ReviewDtos.java
package spl.reborn.study.dto;

import spl.reborn.study.entity.ReviewProgress;

import java.time.LocalDate;

public class ReviewDtos {

    /** (응답) 특정 날짜에 해야 할 복습 목록 아이템 */
    public record DueReviewDto(
            Long reviewProgressId,
            Long userStudyId,
            String contentTitle,
            int stageIndex,
            LocalDate nextReviewDate
    ) {
        public static DueReviewDto from(ReviewProgress rp) {
            return new DueReviewDto(
                    rp.getId(),
                    rp.getUserStudy().getId(),
                    rp.getUserStudy().getContentTitle(),
                    rp.getStageIndex(),
                    rp.getNextReviewDate()
            );
        }
    }

    /** (요청) 이해도 기록 */
    public static class UnderstandingReq {
        public ReviewProgress.UnderstandingResult result;
    }
}
