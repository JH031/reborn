package spl.reborn.problem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimilarProblemDto {
    private String question;
    private String answer;

    @JsonProperty("solution_steps")
    private String solution;
}