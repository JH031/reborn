package spl.reborn.problem.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spl.reborn.problem.dto.*;
import spl.reborn.problem.entity.AnalysisOption;
import spl.reborn.problem.service.ProblemFlowService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemFlowService problemFlowService;

    @Operation(summary = "사용자가 처음 옵션,이미지 보내서 질문할 때 사용")
    @PostMapping(
            value = "/analyze-first",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AnalyzeFirstResponse analyzeFirst(
            @RequestParam Long userId,
            @RequestPart("image") MultipartFile image,
            @RequestParam("option") AnalysisOption option,
            @RequestParam(name = "userRequest", required = false) String userRequest
    ) throws Exception {
        return problemFlowService.analyzeFirst(userId, image, option, userRequest);
    }


    @Operation(summary = "사용자에게 프롬프트와 (선택) 이미지를 받아서 gemini에게 요청")
    @PostMapping(
            value = "/{problemId}/ask",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AnalyzeResponse askFollowUp(
            @RequestParam Long userId,
            @PathVariable Long problemId,
            @RequestParam String prompt,
            @RequestPart(name = "image", required = false) MultipartFile image // 파일 파트
    ) {
        return problemFlowService.analyzeFollowUp(userId, problemId, prompt, image);
    }

    @Operation(summary = "유사한 문제 요청")
    @PostMapping("/{problemId}/similar")
    public ResponseEntity<AnalyzeResponse> generateSimilar(
            @RequestParam Long userId,
            @PathVariable Long problemId
    ) {
        AnalyzeResponse res = problemFlowService.generateSimilarProblemsFromFullAnalysis(userId, problemId);
        return ResponseEntity.ok(res);
    }
}
