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
import spl.reborn.problem.repository.AnalysisRepository;
import spl.reborn.problem.repository.ProblemRepository;
import spl.reborn.problem.support.AnalysisDisplayMapper;
import spl.reborn.s3.service.S3Service;
import spl.reborn.user.entity.User;
import spl.reborn.user.repository.UserRepository;
import spl.reborn.problem.support.SubjectConceptExtractor;
import spl.reborn.problem.support.SubjectConceptRefiner;

// ★ UserStudy 관련
import spl.reborn.study.entity.UserStudy;
import spl.reborn.study.repository.UserStudyRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private final UserStudyRepository userStudyRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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

        String prompt = hint; // GeminiInlineService 내부 기본 프롬프트 존재

        // 3) Gemini 호출 (이미지 URL 인라인)
        String geminiRaw = geminiService.generateFromImageUrl(imageUrl, prompt);

        // 4) Problem 보강(subject/mainConcept 추출 시도 - 실패해도 무시)
        enrichProblem(problem, geminiRaw);

        // ★ 4.5) UserStudy 생성: contentTitle = "subject_mainConcept", imageUrl 포함
        String subject = safe(problem.getSubject());
        String concept = safe(problem.getMainConcept());
        String contentTitle = buildContentTitle(subject, concept);

        UserStudy study = new UserStudy();
        study.setUser(user);
        study.setContentTitle(contentTitle);
        study.setStudyDate(LocalDate.now(KST));
        study.setImageUrl(problem.getImageUrl()); // ★ Problem의 이미지 URL 저장
        userStudyRepository.save(study);

        // 5) Analysis 저장 (turn=1)
        Analysis a = new Analysis();
        a.setProblem(problem);
        a.setTurn(1);
        a.setOption(option);
        a.setUserRequest(null);
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

        // 후속 턴은 사용자 프롬프트 원문 전달
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

        return AnalyzeResponse.builder()
                .analysisId(a.getAnalysisId())
                .turn(nextTurn)
                .option(null)
                .message(geminiRaw)
                .build();
    }

    /** JSON/문장에서 subject/mainConcept를 추출해 Problem에 채움 */
    private void enrichProblem(Problem p, String geminiRaw) {
        var r = SubjectConceptExtractor.extract(geminiRaw, objectMapper);

        // 1차 실패 시 보정 재시도
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

    // ---- helpers ------------------------------------------------------------

    private static boolean isBlank(String s){ return s == null || s.isBlank(); }
    private static boolean notBlank(String s){ return s != null && !s.isBlank(); }
    private static String safe(String s){ return s == null ? "" : s.trim(); }

    /** subject/mainConcept를 '_'로 연결. 둘 다 비면 "문제" 반환 */
    private static String buildContentTitle(String subject, String concept) {
        boolean hasSubj = notBlank(subject);
        boolean hasConcept = notBlank(concept);
        if (hasSubj && hasConcept) return subject + "_" + concept;
        if (hasSubj) return subject;
        if (hasConcept) return concept;
        return "문제";
    }

    private String trimForContext(String s) {
        if (s == null) return "";
        return s.length() > 1500 ? s.substring(0, 1500) + " …(truncated)" : s;
    }
}
