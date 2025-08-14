package spl.reborn.problem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import spl.reborn.ai.prompt.AnalysisPrompts;
import spl.reborn.ai.service.GeminiInlineService;
import spl.reborn.problem.dto.AnalyzeFirstResponse;
import spl.reborn.problem.dto.AnalyzeResponse;
import spl.reborn.problem.entity.Analysis;
import spl.reborn.problem.entity.AnalysisOption;
import spl.reborn.problem.entity.Problem;
import spl.reborn.problem.repository.AnalysisRepository;
import spl.reborn.problem.repository.ProblemRepository;
import spl.reborn.s3.service.S3Service;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemFlowService {

    private final S3Service s3Service;
    private final GeminiInlineService geminiService;
    private final ProblemRepository problemRepository;
    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /** 1) 최초: 이미지+옵션(무조건) */
    @Transactional
    public AnalyzeFirstResponse analyzeFirst(Long userId,
                                             MultipartFile image,
                                             AnalysisOption option,
                                             String userRequestOptional) throws IOException {

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("이미지가 필요합니다.");
        }
        if (option == null) {
            throw new IllegalArgumentException("옵션이 필요합니다. (APPROACH/FULL_SOLUTION/FIND_MY_ERROR)");
        }

        // 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다. id=" + userId));

        // 1) 업로드 & Problem 생성
        String imageUrl = s3Service.uploadFile(image);

        Problem problem = new Problem();
        problem.setUser(user);
        problem.setImageUrl(imageUrl);
        problem.setCreatedAt(LocalDateTime.now());
        // subject, mainConcept은 나중에 응답 JSON에서 추출하여 업데이트
        problemRepository.save(problem);

        // 2) 프롬프트 구성 (옵션별 힌트 추가)
        String hint = switch (option) {
            case APPROACH      -> AnalysisPrompts.APPROACH_HINT;
            case FULL_SOLUTION -> AnalysisPrompts.FULL_SOLUTION_HINT;
            case FIND_MY_ERROR -> AnalysisPrompts.findMyErrorHint(userRequestOptional);
        };

        String prompt = hint; // GeminiInlineService 내부에 기본 시스템 프롬프트(DEFAULT_ANALYSIS_PROMPT)가 존재

        // 3) Gemini 호출 (이미지 URL 인라인)
        String geminiResponse = geminiService.generateFromImageUrl(imageUrl, prompt);

        // 4) Problem 보강(subject/mainConcept 추출 시도 - 실패해도 무시)
        enrichProblemFromJson(problem, geminiResponse);

        // 5) Analysis 저장 (turn=1)
        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(1);
        a.setOption(option);
        a.setUserRequest(null); // 최초 턴은 사용자가 프롬프트를 보내지 않음(요구사항)
        a.setGeminiResponse(geminiResponse);
        a.setCreatedAt(LocalDateTime.now());
        analysisRepository.save(a);

        return AnalyzeFirstResponse.builder()
                .problemId(problem.getProblemId())
                .imageUrl(imageUrl)
                .analysisId(a.getAnalysisId())
                .turn(1)
                .option(option)
                .geminiResponse(geminiResponse)
                .build();
    }

    /** 2) 후속 턴: 사용자 프롬프트만 */
    @Transactional
    public AnalyzeResponse analyzeFollowUp(Long userId, Long problemId, String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("프롬프트가 필요합니다.");
        }

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. id=" + problemId));

        long ownerId = problem.getUser().getId();
        if (userId == null || ownerId != userId) {  // Long → long 오토언박싱, NPE 방지 위해 null 체크
            throw new IllegalStateException("본인 문제에만 후속 요청을 보낼 수 있습니다.");
        }

        // 직전 분석(들) 조회
        var latest = analysisRepository.findTopByProblem_ProblemIdOrderByTurnDesc(problemId);
        if (latest == null) {
            throw new IllegalStateException("최초 분석이 없습니다. 먼저 이미지+옵션으로 분석을 실행하세요.");
        }
        int nextTurn = latest.getTurn() + 1;

        // 최근 1~2개를 요약 컨텍스트로 사용
        List<Analysis> recents = analysisRepository.findTop2ByProblem_ProblemIdOrderByTurnDesc(problemId);
        StringBuilder recentSummaries = new StringBuilder();
        for (Analysis r : recents) {
            recentSummaries.append("[turn=")
                    .append(r.getTurn())
                    .append(", option=")
                    .append(r.getOption())
                    .append("]\n")
                    .append(trimForContext(r.getGeminiResponse()))
                    .append("\n\n");
        }

        String followUpPrompt = AnalysisPrompts.followUp(recentSummaries.toString(), userPrompt);

        // 후속은 텍스트만 전송
        String geminiResponse = geminiService.generateFromText(followUpPrompt);

        // 저장(option=null)
        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(nextTurn);
        a.setOption(null);
        a.setUserRequest(userPrompt);
        a.setGeminiResponse(geminiResponse);
        a.setCreatedAt(LocalDateTime.now());
        analysisRepository.save(a);

        // 필요시 보강 업데이트 시도(새 정보가 있으면)
        enrichProblemFromJson(problem, geminiResponse);

        return AnalyzeResponse.builder()
                .analysisId(a.getAnalysisId())
                .turn(nextTurn)
                .option(null)
                .geminiResponse(geminiResponse)
                .build();
    }

    /** JSON에서 subject/mainConcept 추출 시도 (실패해도 예외없이 무시) */
    private void enrichProblemFromJson(Problem p, String jsonLike) {
        try {
            Map<String, Object> map = objectMapper.readValue(jsonLike, new TypeReference<Map<String, Object>>() {});
            Object subject = map.get("subject");
            if (subject instanceof String s && !s.isBlank()) {
                p.setSubject(s);
            }
            // mainConcept 후보: needed_concepts[0] 이나 problem_summary에서 키워드 추출 등
            Object needed = map.get("needed_concepts");
            if (needed instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof String s && !s.isBlank()) {
                    p.setMainConcept(s);
                }
            }
            // 저장
            problemRepository.save(p);
        } catch (Exception ignore) {
            // Gemini 응답이 JSON이 아니거나 필드가 없으면 조용히 패스
        }
    }

    private String trimForContext(String s) {
        if (s == null) return "";
        // 최근 컨텍스트로 1500자 정도만 사용 (토큰 절약)
        return s.length() > 1500 ? s.substring(0, 1500) + " …(truncated)" : s;
    }
}
