package spl.reborn.ai.controller;

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

    @PostMapping("/generate-from-url")
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
