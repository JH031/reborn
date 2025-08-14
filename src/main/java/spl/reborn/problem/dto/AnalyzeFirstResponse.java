package spl.reborn.problem.dto;

import lombok.*;
import spl.reborn.problem.entity.AnalysisOption;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyzeFirstResponse {
    private Long problemId;
    private String imageUrl;
    private Long analysisId;
    private int turn; // 항상 1
    private AnalysisOption option;
    private String geminiResponse;
}
