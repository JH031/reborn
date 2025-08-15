package spl.reborn.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ChatHistoryResponse {
    private Long problemId;
    private String title;
    private String originalImageUrl;
    private List<ChatTurnDto> chatTurns;
}