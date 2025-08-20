package spl.reborn.problem.dto; // 실제 프로젝트 패키지 경로에 맞게 수정하세요.

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import spl.reborn.problem.entity.ResponseType;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnifiedProblemResponseDto {

    private Long analysisId;
    private Integer turn;
    private ResponseType responseType; // 응답의 종류 (ANALYSIS, SIMILAR_PROBLEMS, TEXT)

    private Map<String, Object> display;
    private List<SimilarProblemDto> similarProblems;
    private String message;
}