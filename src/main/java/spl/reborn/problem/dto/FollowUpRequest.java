package spl.reborn.problem.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FollowUpRequest {
    private String prompt; // 후속 턴 사용자 프롬프트(필수)
}