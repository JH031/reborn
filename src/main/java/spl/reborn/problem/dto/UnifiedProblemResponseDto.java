package spl.reborn.problem.dto; // 실제 프로젝트 패키지 경로에 맞게 수정하세요.

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import spl.reborn.problem.entity.ResponseType;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 응답 JSON에서 자동으로 제외됩니다.
public class UnifiedProblemResponseDto {

    private Long analysisId;
    private Integer turn;
    private ResponseType responseType; // 응답의 종류 (ANALYSIS, SIMILAR_PROBLEMS, TEXT)

    // --- 내용 영역 (responseType에 따라 이 중 하나만 값이 채워집니다) ---
    private Map<String, Object> display;
    private List<SimilarProblemDto> similarProblems;
    private String message;
}