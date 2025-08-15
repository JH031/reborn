package spl.reborn.problem.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SimilarProblemResponseDto {
    private List<SimilarProblemDto> problems;
}