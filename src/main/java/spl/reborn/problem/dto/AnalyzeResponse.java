package spl.reborn.problem.dto;

import lombok.*;
import spl.reborn.problem.entity.AnalysisOption;
import spl.reborn.problem.entity.SimilarOption;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyzeResponse {
    private Long analysisId;
    private int turn;
    private AnalysisOption option; // 후속 턴은 null
    private SimilarOption similarOption;
    private String message;
    private List<SimilarProblemDto> similarProblems;
}