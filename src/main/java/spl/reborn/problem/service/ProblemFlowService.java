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
import spl.reborn.problem.dto.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// ★ 추가: user_study 저장을 위한 import
import java.time.LocalDate;
import spl.reborn.study.entity.UserStudy;
import spl.reborn.study.repository.UserStudyRepository;

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

    // ★ 추가: user_study 저장용 리포지토리
    private final UserStudyRepository userStudyRepository;

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
        problem.setOriginalImageUrl(imageUrl); // ✅ 대표/최초 이미지
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
        String geminiRaw = geminiService.generateFromImageUrl(imageUrl, prompt, true);

        // 4) Problem 보강(subject/mainConcept 추출 시도 - 실패해도 무시)
        enrichProblem(problem, geminiRaw);

        // 5) Analysis 저장 (turn=1) — 히스토리 일관성 위해 imageUrl도 기록
        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(1);
        a.setOption(option);
        a.setUserRequest(null); // 최초 턴은 사용자가 프롬프트를 보내지 않음(요구사항)
        a.setGeminiResponse(geminiRaw);
        a.setImageUrl(imageUrl); // ✅ turn=1 이미지도 기록
        a.setCreatedAt(LocalDateTime.now());
        analysisRepository.save(a);

        // ★★★★★ 추가: user_study 자동 저장
        // 제목 우선순위: mainConcept → subject → "제목 없음"
        String contentTitle =
                (problem.getMainConcept() != null && !problem.getMainConcept().isBlank()) ? problem.getMainConcept() :
                        (problem.getSubject() != null && !problem.getSubject().isBlank()) ? problem.getSubject() :
                                "제목 없음";

        UserStudy study = new UserStudy();
        study.setUser(user);
        study.setContentTitle(contentTitle);
        study.setStudyDate(LocalDate.now());
        study.setImageUrl(imageUrl);
        userStudyRepository.save(study);
        log.info("[UserStudy] created id={}, user={}, title='{}', date={}, img={}",
                study.getId(), user.getId(), contentTitle, study.getStudyDate(), study.getImageUrl());

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

    /** 2-1) 후속 턴(텍스트만) — 호환용 */
    @Transactional
    public AnalyzeResponse analyzeFollowUp(Long userId, Long problemId, String userPrompt) {
        return analyzeFollowUp(userId, problemId, userPrompt, null);
    }

    /** 2-2) 후속 턴(프롬프트 + 선택 이미지) */
    @Transactional
    public AnalyzeResponse analyzeFollowUp(Long userId, Long problemId, String userPrompt, MultipartFile image) {
        if (userPrompt == null || userPrompt.isBlank()) throw new IllegalArgumentException("프롬프트가 필요합니다.");

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. id=" + problemId));

        Long ownerId = problem.getUser().getId();
        if (userId == null || !ownerId.equals(userId)) {
            throw new IllegalStateException("본인 문제에만 후속 요청을 보낼 수 있습니다.");
        }

        Analysis latest = analysisRepository.findTopByProblem_ProblemIdOrderByTurnDesc(problemId);
        if (latest == null) throw new IllegalStateException("최초 분석이 없습니다. 먼저 analyzeFirst를 실행하세요.");

        int nextTurn = latest.getTurn() + 1;

        // ✅ 사용자의 원본 프롬프트에 한국어 답변을 요청하는 구문을 추가합니다.
        String finalPrompt = userPrompt + "\n\n(답변은 반드시 한국어로 작성해주세요.)";

        String geminiRaw;
        String imageUrlForThisTurn = null;

        if (image != null && !image.isEmpty()) {
            try {
                imageUrlForThisTurn = s3Service.uploadFile(image);
            } catch (IOException e) {
                throw new RuntimeException("이미지 업로드 실패", e);
            }
            // ✅ 수정된 finalPrompt를 전달합니다.
            geminiRaw = geminiService.generateFromImageUrl(imageUrlForThisTurn, finalPrompt, false);
        } else {
            // ✅ 수정된 finalPrompt를 전달합니다.
            geminiRaw = geminiService.generateFromText(finalPrompt, false);
        }

        // DB 저장 (항상 원문 저장, 이번 턴 이미지 URL도 저장)
        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(nextTurn);
        a.setOption(null);
        a.setUserRequest(userPrompt);
        a.setGeminiResponse(geminiRaw);
        a.setImageUrl(imageUrlForThisTurn);     // null 가능(이미지 없으면)
        a.setCreatedAt(LocalDateTime.now());
        analysisRepository.save(a);

        return AnalyzeResponse.builder()
                .analysisId(a.getAnalysisId())
                .turn(nextTurn)
                .option(null)
                .message(geminiRaw)             // ✅ Gemini 원문 그대로 반환
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
        // ... (기존 그대로)
        // 변경 없음
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. id=" + problemId));

        long ownerId = problem.getUser().getId();
        if (userId == null || ownerId != userId) {
            throw new IllegalStateException("본인 문제에만 유사문제를 생성할 수 있습니다.");
        }

        Analysis latest = analysisRepository.findTopByProblem_ProblemIdOrderByTurnDesc(problemId);
        if (latest == null) {
            throw new IllegalStateException("최초 분석이 없습니다. 먼저 analyzeFirst를 실행하세요.");
        }

        String prompt = """
다음은 원문 문제에 대한 상세 분석(JSON)입니다:
%s

%s
""".formatted(latest.getGeminiResponse(), AnalysisPrompts.SIMILAR_PROBLEMS_FROM_FULL_ANALYSIS);

        String rawJson = geminiService.generateFromText(prompt);
        String cleanedJson = cleanGeminiResponse(rawJson);

        List<SimilarProblemDto> similarProblems;
        try {
            SimilarProblemResponseDto responseDto = objectMapper.readValue(cleanedJson, SimilarProblemResponseDto.class);
            similarProblems = responseDto.getProblems();
            if (similarProblems == null) similarProblems = List.of();
        } catch (IOException e) {
            log.error("Gemini 유사 문제 JSON 파싱에 실패했습니다: {}", cleanedJson, e);
            similarProblems = List.of();
        }

        int nextTurn = latest.getTurn() + 1;
        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(nextTurn);
        a.setOption(null);
        a.setSimilarOption(SimilarOption.SIMILAR_PROBLEMS);
        a.setUserRequest("유사문제 2개 생성 (전체 분석 참고)");
        a.setGeminiResponse(rawJson);
        a.setCreatedAt(java.time.LocalDateTime.now());
        analysisRepository.save(a);

        return AnalyzeResponse.builder()
                .analysisId(a.getAnalysisId())
                .turn(nextTurn)
                .option(null)
                .similarOption(SimilarOption.SIMILAR_PROBLEMS)
                .similarProblems(similarProblems)
                .build();
    }

    private String cleanGeminiResponse(String response) {
        if (response == null) {
            return null;
        }
        String cleaned = response.trim();
        if (cleaned.startsWith("```json") && cleaned.endsWith("```")) {
            cleaned = cleaned.substring("```json".length(), cleaned.length() - "```".length()).trim();
        } else if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            cleaned = cleaned.substring("```".length(), cleaned.length() - "```".length()).trim();
        }
        return cleaned;
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(Long userId, Long problemId) {
        // ... (기존 그대로)
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. id=" + problemId));

        if (problem.getUser().getId() != userId) {
            throw new IllegalStateException("본인의 문제에 대한 채팅 내역만 조회할 수 있습니다.");
        }

        List<Analysis> analyses = analysisRepository.findByProblem_ProblemIdOrderByTurnAsc(problemId);
        if (analyses.isEmpty()) {
            throw new IllegalStateException("해당 문제에 대한 분석 내역이 없습니다.");
        }

        Analysis firstTurn = analyses.get(0);
        String title = "제목 없음";
        if (firstTurn.getTurn() == 1 && firstTurn.getGeminiResponse() != null) {
            try {
                String cleanedJson = cleanGeminiResponse(firstTurn.getGeminiResponse());
                Map<String, Object> firstResponseMap = objectMapper.readValue(cleanedJson, new TypeReference<>() {});
                title = (String) firstResponseMap.getOrDefault("problem_summary", "제목 없음");
            } catch (IOException e) {
                log.warn("turn=1 분석 결과의 JSON 파싱 실패, problemId={}", problemId, e);
            }
        }

        List<ChatTurnDto> chatTurns = new ArrayList<>();
        for (Analysis analysis : analyses) {
            if (analysis.getUserRequest() != null && !analysis.getUserRequest().isBlank()) {
                chatTurns.add(new ChatTurnDto(
                        analysis.getTurn(),
                        "user",
                        analysis.getUserRequest(),
                        null,
                        analysis.getCreatedAt()
                ));
            }

            chatTurns.add(new ChatTurnDto(
                    analysis.getTurn(),
                    "model",
                    analysis.getGeminiResponse(),
                    analysis.getImageUrl(),
                    analysis.getCreatedAt()
            ));
        }

        return new ChatHistoryResponse(
                problem.getProblemId(),
                title,
                problem.getOriginalImageUrl(),
                chatTurns
        );
    }
}
