package spl.reborn.problem.service;

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
import spl.reborn.problem.entity.SimilarOption;
import spl.reborn.problem.repository.AnalysisRepository;
import spl.reborn.problem.repository.ProblemRepository;
import spl.reborn.problem.support.AnalysisDisplayMapper;
import spl.reborn.s3.service.S3Service;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;
import spl.reborn.problem.support.SubjectConceptExtractor;
import spl.reborn.problem.support.SubjectConceptRefiner;

import java.io.IOException;
import java.time.LocalDateTime;
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
        String geminiRaw = geminiService.generateFromImageUrl(imageUrl, prompt);

        // 4) Problem 보강(subject/mainConcept 추출 시도 - 실패해도 무시)
        enrichProblem(problem, geminiRaw);

        // 5) Analysis 저장 (turn=1)
        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(1);
        a.setOption(option);
        a.setUserRequest(null); // 최초 턴은 사용자가 프롬프트를 보내지 않음(요구사항)
        a.setGeminiResponse(geminiRaw);
        a.setCreatedAt(LocalDateTime.now());
        analysisRepository.save(a);

        Map<String, Object> display = AnalysisDisplayMapper.toDisplay(geminiRaw, option, objectMapper);
        return AnalyzeFirstResponse.builder()
                .problemId(problem.getProblemId())
                .imageUrl(imageUrl)
                .analysisId(a.getAnalysisId())
                .turn(1)
                .option(option)
                .display(display)
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
        if (userId == null || ownerId != userId) {
            throw new IllegalStateException("본인 문제에만 후속 요청을 보낼 수 있습니다.");
        }

        var latest = analysisRepository.findTopByProblem_ProblemIdOrderByTurnDesc(problemId);
        if (latest == null) {
            throw new IllegalStateException("최초 분석이 없습니다. 먼저 이미지+옵션으로 분석을 실행하세요.");
        }
        int nextTurn = latest.getTurn() + 1;

        // ✅ 후속 턴은 "사용자 프롬프트를 그대로" 모델에 전달하고, 받은 원문을 그대로 반환
        String geminiRaw = geminiService.generateFromText(userPrompt);

        // DB 저장 (항상 원문 저장, option=null)
        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(nextTurn);
        a.setOption(null);
        a.setUserRequest(userPrompt);
        a.setGeminiResponse(geminiRaw);
        a.setCreatedAt(LocalDateTime.now());
        analysisRepository.save(a);

        // 후속 턴은 원문 Q&A 성격이므로 Problem(subject/mainConcept) 보강은 스킵
        // (원하면 enrichProblem(problem, geminiRaw) 호출로 유지 가능)

        return AnalyzeResponse.builder()
                .analysisId(a.getAnalysisId())
                .turn(nextTurn)
                .option(null)
                .message(geminiRaw) // ✅ 프론트로 원문 그대로
                .build();
    }

    /** JSON/문장에서 subject/mainConcept를 추출해 Problem에 채움 */
    private void enrichProblem(Problem p, String geminiRaw) {
        SubjectConceptExtractor.Result r = SubjectConceptExtractor.extract(geminiRaw, objectMapper);

        // 1차 시도 실패면 가볍게 재요청해서 JSON만 받기
        if (r == null) {
            try {
                String refined = SubjectConceptRefiner.refine(geminiService, geminiRaw);
                r = SubjectConceptExtractor.extract(refined, objectMapper);
            } catch (Exception ignore) {}
        }

        if (r == null) return;

        boolean changed = false;

        // subject: 비어있으면 채우고, 다른 값이면 최신으로 갱신할지 정책 선택
        if (isBlank(p.getSubject()) && notBlank(r.subject())) {
            p.setSubject(r.subject());
            changed = true;
        }

        // mainConcept도 동일 정책
        if (isBlank(p.getMainConcept()) && notBlank(r.mainConcept())) {
            p.setMainConcept(r.mainConcept());
            changed = true;
        }

        if (changed) {
            problemRepository.save(p);
        }
    }

    private static boolean isBlank(String s){ return s == null || s.isBlank(); }
    private static boolean notBlank(String s){ return s != null && !s.isBlank(); }

    @Transactional
    public AnalyzeResponse generateSimilarProblemsFromFullAnalysis(Long userId, Long problemId) {
        // 1) 소유자 검증
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. id=" + problemId));

        long ownerId = problem.getUser().getId();
        if (userId == null || ownerId != userId) {
            throw new IllegalStateException("본인 문제에만 유사문제를 생성할 수 있습니다.");
        }

        // 2) 최신 분석 가져오기
        Analysis latest = analysisRepository.findTopByProblem_ProblemIdOrderByTurnDesc(problemId);
        if (latest == null) {
            throw new IllegalStateException("최초 분석이 없습니다. 먼저 analyzeFirst를 실행하세요.");
        }

        // 3) 프롬프트 (전체 분석 내용 + 템플릿)
        String prompt = """
다음은 원문 문제에 대한 상세 분석(JSON)입니다:
%s

%s
""".formatted(latest.getGeminiResponse(), AnalysisPrompts.SIMILAR_PROBLEMS_FROM_FULL_ANALYSIS);

        // 4) Gemini 호출
        String rawJson = geminiService.generateFromText(prompt);

        // 5) 저장 (turn + 1, option=null, similarOption=SIMILAR_PROBLEMS)
        int nextTurn = latest.getTurn() + 1;
        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(nextTurn);
        a.setOption(null); // ✅ 기존 옵션은 사용 안 함
        a.setSimilarOption(SimilarOption.SIMILAR_PROBLEMS); // ✅ 새 필드
        a.setUserRequest("유사문제 2개 생성 (전체 분석 참고)");
        a.setGeminiResponse(rawJson);
        a.setCreatedAt(java.time.LocalDateTime.now());
        analysisRepository.save(a);

        // 6) 응답
        return AnalyzeResponse.builder()
                .analysisId(a.getAnalysisId())
                .turn(nextTurn)
                .option(null)
                .similarOption(SimilarOption.SIMILAR_PROBLEMS) // ✅ 내려주기
                .message(rawJson)
                .build();
    }

}
