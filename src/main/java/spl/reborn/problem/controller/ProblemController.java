package spl.reborn.problem.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    /** 2) 후속 턴: 사용자 프롬프트만 */
    @PostMapping(value = "/{problemId}/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AnalyzeResponse askFollowUp(
            @RequestParam Long userId,        // TODO: JWT로 대체 권장
            @PathVariable Long problemId,
            @RequestBody FollowUpRequest body  // { "prompt": "..." }
    ) {
        return problemFlowService.analyzeFollowUp(userId, problemId, body.getPrompt());
    }
}
