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
import spl.reborn.problem.entity.*;
import spl.reborn.problem.repository.AnalysisRepository;
import spl.reborn.problem.repository.ProblemRepository;
import spl.reborn.problem.support.AnalysisDisplayMapper;
import spl.reborn.s3.service.S3Service;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;
import spl.reborn.problem.support.SubjectConceptExtractor;
import spl.reborn.problem.support.SubjectConceptRefiner;
// ★ 리마인더 서비스 사용 (기존 그대로)
import spl.reborn.notification.ReminderService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// ★ user_study 저장 관련
import java.time.LocalDate;
import spl.reborn.study.entity.UserStudy;
import spl.reborn.study.repository.UserStudyRepository;

// ★★★ ADDED: review_progress 직접 생성용
import spl.reborn.study.entity.ReviewProgress;                      // ★★★ ADDED
import spl.reborn.study.repository.ReviewProgressRepository;        // ★★★ ADDED

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

    // user_study 저장 리포지토리 (기존)
    private final UserStudyRepository userStudyRepository;

    // 리마인더 자동 생성 (기존)
    private final ReminderService reminderService;

    // ★★★ ADDED: review_progress 저장 리포지토리
    private final ReviewProgressRepository reviewProgressRepository;   // ★★★ ADDED

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

        // ★★★★★ user_study 자동 저장 (기존)
        String subj = problem.getSubject();
        String main = problem.getMainConcept();

        String contentTitle;
        if (subj != null && !subj.isBlank() && main != null && !main.isBlank()) {
            contentTitle = subj.trim() + "_" + main.trim();   // 둘 다 있으면 "subject_mainconcept"
        } else if (main != null && !main.isBlank()) {
            contentTitle = main.trim();                       // main만 있으면 main
        } else if (subj != null && !subj.isBlank()) {
            contentTitle = subj.trim();                       // subject만 있으면 subject
        } else {
            contentTitle = "제목 없음";
        }

        UserStudy study = new UserStudy();
        study.setUser(user);
        study.setContentTitle(contentTitle);
        study.setStudyDate(LocalDate.now());
        study.setImageUrl(imageUrl);
        userStudyRepository.save(study);
        log.info("[UserStudy] created id={}, user={}, title='{}', date={}, img={}",
                study.getId(), user.getId(), contentTitle, study.getStudyDate(), study.getImageUrl());

        // ★★★ ADDED: review_progress 초기 행 생성 (옵션 B)
        try {
            ReviewProgress rp = new ReviewProgress();
            rp.setUserStudy(study);
            rp.setOwner(user);
            rp.setStageIndex(0);
            rp.setCompleted(false);
            rp.setLastResult(null);
            rp.setNextReviewDate(study.getStudyDate().plusDays(1));  // 첫 복습일 = +1일

            rp.setImageUrl(study.getImageUrl());                     // ★★★ ADD: 이미지 URL 복사

            reviewProgressRepository.save(rp);
            log.info("[ReviewProgress] created id={}, userStudyId={}, next={}",
                    rp.getId(), study.getId(), rp.getNextReviewDate());
        } catch (Exception e) {
            log.error("[ReviewProgress] create failed: studyId={}, err={}", study.getId(), e.toString(), e);
        }

        // 리마인더 **자동 생성** (기존 로직 그대로 유지)
        try {
            reminderService.createForStudy(
                    user.getId(),
                    study.getId(),          // contentId: UserStudy PK
                    study.getContentTitle(),
                    study.getStudyDate(),   // 기준일
                    study.getImageUrl()     // (Reminder 쪽에서 이미지 인자는 무시/오버로드 처리 가능)
            );
            log.info("[Reminder] scheduled for user={}, contentId={}, title='{}'",
                    user.getId(), study.getId(), contentTitle);
        } catch (Exception e) {
            log.error("[Reminder] schedule failed user={}, contentId={}, err={}",
                    user.getId(), study.getId(), e.toString(), e);
        }

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

    @Transactional
    public AnalyzeResponse analyzeFollowUp(Long userId, Long problemId, String userPrompt) {
        return analyzeFollowUp(userId, problemId, userPrompt, null);
    }

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

        String finalPrompt = userPrompt + "\n\n(답변은 반드시 한국어로 작성해주세요.)";

        String geminiRaw;
        String imageUrlForThisTurn = null;

        if (image != null && !image.isEmpty()) {
            try {
                imageUrlForThisTurn = s3Service.uploadFile(image);
            } catch (IOException e) {
                throw new RuntimeException("이미지 업로드 실패", e);
            }
            geminiRaw = geminiService.generateFromImageUrl(imageUrlForThisTurn, finalPrompt, false);
        } else {
            geminiRaw = geminiService.generateFromText(finalPrompt, false);
        }


        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(nextTurn);
        a.setOption(null);
        a.setUserRequest(userPrompt);
        a.setGeminiResponse(geminiRaw);
        a.setImageUrl(imageUrlForThisTurn);
        a.setCreatedAt(LocalDateTime.now());
        analysisRepository.save(a);

        return AnalyzeResponse.builder()
                .analysisId(a.getAnalysisId())
                .turn(nextTurn)
                .option(null)
                .message(geminiRaw)
                .build();
    }

    private void enrichProblem(Problem p, String geminiRaw) {
        SubjectConceptExtractor.Result r = SubjectConceptExtractor.extract(geminiRaw, objectMapper);

        if (r == null) {
            try {
                String refined = SubjectConceptRefiner.refine(geminiService, geminiRaw);
                r = SubjectConceptExtractor.extract(refined, objectMapper);
            } catch (Exception ignore) {}
        }

        if (r == null) return;

        boolean changed = false;

        if (isBlank(p.getSubject()) && notBlank(r.subject())) {
            p.setSubject(r.subject());
            changed = true;
        }

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

        String escapedJson = cleanedJson.replace("\\", "\\\\");

        List<SimilarProblemDto> similarProblems;
        try {
            SimilarProblemResponseDto responseDto = objectMapper.readValue(escapedJson, SimilarProblemResponseDto.class);
            similarProblems = responseDto.getProblems();
            if (similarProblems == null) similarProblems = List.of();
        } catch (IOException e) {
            log.error("Gemini 유사 문제 JSON 파싱에 실패했습니다: {}", escapedJson, e);
            similarProblems = List.of();
        }

        int nextTurn = latest.getTurn() + 1;
        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(nextTurn);
        a.setOption(null);
        a.setSimilarOption(SimilarOption.SIMILAR_PROBLEMS);
        a.setUserRequest(null);
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
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. id=" + problemId));

        if (problem.getUser().getId() != userId) {
            throw new IllegalStateException("본인의 문제에 대한 채팅 내역만 조회할 수 있습니다.");
        }

        List<Analysis> analyses = analysisRepository.findByProblem_ProblemIdOrderByTurnAsc(problemId);
        if (analyses.isEmpty()) {
            throw new IllegalStateException("해당 문제에 대한 분석 내역이 없습니다.");
        }

        List<ChatTurnDto> chatTurns = new ArrayList<>();
        for (Analysis analysis : analyses) {
            if (analysis.getUserRequest() != null && !analysis.getUserRequest().isBlank()) {
                chatTurns.add(new ChatTurnDto(
                        analysis.getTurn(), "user", analysis.getUserRequest(), null, analysis.getCreatedAt()
                ));
            }

            Object modelContent = convertAnalysisToStructuredContent(analysis);
            chatTurns.add(new ChatTurnDto(
                    analysis.getTurn(), "model", modelContent, analysis.getImageUrl(), analysis.getCreatedAt()
            ));
        }

        return new ChatHistoryResponse(
                problem.getProblemId(),
                problem.getOriginalImageUrl(),
                chatTurns
        );
    }

    @Transactional(readOnly = true)
    public List<ProblemSummaryDto> getProblemList(Long userId) {
        List<Problem> problems = problemRepository.findByUser_IdOrderByCreatedAtDesc(userId);

        List<ProblemSummaryDto> problemSummaries = new ArrayList<>();

        for (Problem problem : problems) {
            String title = analysisRepository.findFirstByProblem_ProblemIdOrderByTurnAsc(problem.getProblemId())
                    .map(firstAnalysis -> {
                        if (firstAnalysis.getGeminiResponse() != null) {
                            try {
                                String cleanedJson = cleanGeminiResponse(firstAnalysis.getGeminiResponse());
                                Map<String, Object> responseMap = objectMapper.readValue(cleanedJson, new TypeReference<>() {});
                                return (String) responseMap.getOrDefault("problem_summary", "제목 없음");
                            } catch (IOException e) {
                                log.warn("problemId={}의 제목 파싱 실패", problem.getProblemId(), e);
                                return "제목 파싱 실패";
                            }
                        }
                        return "분석 내용 없음";
                    })
                    .orElse("제목 없음");

            problemSummaries.add(new ProblemSummaryDto(
                    problem.getProblemId(),
                    title,
                    problem.getCreatedAt()
            ));
        }

        return problemSummaries;
    }

    private Object convertAnalysisToStructuredContent(Analysis analysis) {
        if (analysis.getOption() == null && analysis.getSimilarOption() == null) {
            return UnifiedProblemResponseDto.builder()
                    .analysisId(analysis.getAnalysisId())
                    .turn(analysis.getTurn())
                    .responseType(ResponseType.TEXT)
                    .message(analysis.getGeminiResponse())
                    .build();
        }

        if (analysis.getSimilarOption() == SimilarOption.SIMILAR_PROBLEMS) {
            try {
                String cleanedJson = cleanGeminiResponse(analysis.getGeminiResponse());
                SimilarProblemResponseDto responseDto = objectMapper.readValue(cleanedJson, SimilarProblemResponseDto.class);
                List<SimilarProblemDto> problems = responseDto.getProblems() != null ? responseDto.getProblems() : List.of();
                return UnifiedProblemResponseDto.builder()
                        .analysisId(analysis.getAnalysisId())
                        .turn(analysis.getTurn())
                        .responseType(ResponseType.SIMILAR_PROBLEMS)
                        .similarProblems(problems)
                        .build();
            } catch (IOException e) {
                return analysis.getGeminiResponse();
            }
        }

        if (analysis.getOption() != null) {
            try {
                Map<String, Object> displayMap = AnalysisDisplayMapper.toDisplay(analysis.getGeminiResponse(), analysis.getOption(), objectMapper);
                return UnifiedProblemResponseDto.builder()
                        .analysisId(analysis.getAnalysisId())
                        .turn(analysis.getTurn())
                        .responseType(ResponseType.ANALYSIS)
                        .display(displayMap)
                        .build();
            } catch (Exception e) {
                return analysis.getGeminiResponse();
            }
        }

        return analysis.getGeminiResponse();
    }
}
