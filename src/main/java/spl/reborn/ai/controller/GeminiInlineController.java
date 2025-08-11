package spl.reborn.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spl.reborn.ai.service.GeminiInlineService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class GeminiInlineController {

    private final GeminiInlineService service;

    @Operation(summary = "사진 넘기고 gemini 대답 받아오기")
    @PostMapping("/ans_from_gemini")
    public ResponseEntity<String> generateFromUrl(@RequestBody Req req) {
        String result = service.generateFromImageUrl(req.getUrl(), req.getPrompt());
        return ResponseEntity.ok(result);
    }

    @Data
    public static class Req {
        private String url;     // S3 퍼블릭 또는 프리사인드 URL
        private String prompt;  // 선택
    }
}
