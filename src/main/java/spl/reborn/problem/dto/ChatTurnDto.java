package spl.reborn.problem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // imageUrl이 null이면 응답에서 제외
public class ChatTurnDto {
    private int turn;
    private String role; // "user" 또는 "model"
    private String content;
    private String imageUrl;
}