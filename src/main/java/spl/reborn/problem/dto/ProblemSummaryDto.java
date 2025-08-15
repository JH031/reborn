package spl.reborn.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ProblemSummaryDto {
    private Long problemId;
    private String title;
    private LocalDateTime createdAt;
}