package spl.reborn.problem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatTurnDto {
    private int turn;
    private String role; // "user" 또는 "model"
    private Object content;
    private String imageUrl;
    private LocalDateTime createdAt;
}