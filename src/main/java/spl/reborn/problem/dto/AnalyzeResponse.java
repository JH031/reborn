package spl.reborn.problem.dto;

import lombok.*;
import spl.reborn.problem.entity.AnalysisOption;

import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyzeResponse {
    private Long analysisId;
    private int turn;
    private AnalysisOption option; // 후속 턴은 null
    private String message;
}