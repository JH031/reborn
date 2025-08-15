package spl.reborn.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.URLConnection;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GeminiInlineService {

    private final WebClient http; // 이미지 다운로드 & Gemini 호출

    @Value("${gemini.api.url}") // ex) https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiInlineService() {
        // 25MB 버퍼 + 타임아웃 설정
        var strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(25 * 1024 * 1024))
                .build();

        HttpClient client = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30));

        this.http = WebClient.builder()
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(client)) // ✅ 여기 수정
                .build();
    }

    private static final String DEFAULT_ANALYSIS_PROMPT = """
역할: 너는 초/중/고 모든 과목(수학, 과학, 국어, 영어, 사회, 예체능 포함)의 숙제·시험 채점관이자 과외 선생님이다.
입력 이미지는 학생이 촬영한 문제지, 필기, 시험지, 숙제 사진일 수 있다.

해야 할 일:
- subject: 이미지 속 문제의 과목을 추론 (예: "수학", "과학", "국어", "영어", "사회", "기타")
- has_solution: 사진에 학생의 풀이(수식, 서술, 그림, 계산 과정)가 있는지 판별 (true/false)
- solution_evidence: 풀이 존재 여부의 근거 설명
- problem_summary: 문제를 짧게 요약
- given: 문제에서 주어진 정보나 조건
- asked: 문제에서 구하는 것 또는 요구사항
  - is_correct: 정답이 있다면 정답 여부 (true/false/unknown)
  - error_analysis: 풀이 과정이 있다면 어디서 왜 틀렸는지 설명(산술/개념/논리/표현/단위 등 유형 표시)
  - correct_answer: 올바른 최종 답
  - step_by_step_explain: 올바른 풀이 과정을 간단하게
  - needed_concepts: 풀기 위해 필요한 개념 목록
  - how_to_approach: 접근 절차(문제 분석 → 개념 적용 → 해결) 요약
- feedback: 학생에게 줄 짧고 친절한 피드백 (2문장 이내)
- confidence: 전체 판단 신뢰도(0~1)

출력 형식: 아래의 JSON만 출력하라.
{
  "subject": "…",
  "has_solution": true,
  "solution_evidence": "…",
  "problem_summary": "…",
  "given": ["…"],
  "asked": "…",
  "is_correct": true,
  "error_analysis": [
    {"type": "arithmetic|concept|logic|expression|units", "explanation": "…"}
  ],
  "correct_answer": "…",
  "step_by_step_explain": "…",
  "needed_concepts": ["…"],
  "how_to_approach": "…",
  "feedback": "…",
  "confidence": 0.0
}

제약:
- 모든 출력은 한국어로 작성.
- 불명확하면 null 또는 "unknown" 사용.
- 여러 문제가 보이면 가장 명확한 1~2개만 처리.
- 수식은 간단한 LaTeX 또는 평문 사용.
- 각 필드는 간결히 작성.
- 절대 ```json 같은 코드블록으로 감싸지 말고, 순수 JSON만 출력하라.
""";

    /** 퍼블릭/프리사인드 S3 URL을 인라인(Base64)로 변환해 Gemini 호출 */
    public String generateFromImageUrl(String imageUrl, String prompt) {
        return generateFromImageUrl(imageUrl, prompt, true);
    }

    /** 새 메서드: 기본 프롬프트 prepend 여부를 호출자가 제어 */
    public String generateFromImageUrl(String imageUrl, String prompt, boolean prependDefault) {
        byte[] bytes = http.get()
                .uri(imageUrl)
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).map(body ->
                                new RuntimeException("Image GET failed " + resp.statusCode() + ": " + body)))
                .bodyToMono(byte[].class)
                .block();

        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("이미지 다운로드 실패 또는 빈 파일 (url=" + imageUrl + ")");
        }

        String mime = guessMime(bytes, imageUrl);
        String b64  = java.util.Base64.getEncoder().encodeToString(bytes);

        String instruction;
        if (prependDefault) {
            instruction = (prompt == null || prompt.isBlank())
                    ? DEFAULT_ANALYSIS_PROMPT
                    : DEFAULT_ANALYSIS_PROMPT + "\n\n" + prompt;
        } else {
            // 후속턴(이미지 포함)에서 기본 프롬프트를 붙이지 않음
            instruction = (prompt == null) ? "" : prompt;
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("inline_data", Map.of(
                                        "mime_type", mime,
                                        "data", b64
                                )),
                                Map.of("text", instruction)
                        ))
                )
        );

        Map<?, ?> res = http.post()
                .uri(apiUrl + "?key=" + apiKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).map(bodyText ->
                                new RuntimeException("Gemini POST failed " + resp.statusCode() + ": " + bodyText)))
                .bodyToMono(Map.class)
                .block();

        return extractText(res);
    }

    private String guessMime(byte[] bytes, String url) {
        try {
            var in = new java.io.ByteArrayInputStream(bytes);
            String byContent = URLConnection.guessContentTypeFromStream(in);
            if (byContent != null) return byContent;
        } catch (Exception ignored) {}
        String lower = url.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif"))  return "image/gif";
        return "image/jpeg";
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> res) {
        if (res == null) return "(no response)";
        try {
            var candidates = (List<Map<String, Object>>) res.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "(no candidates)";
            var content = (Map<String, Object>) candidates.get(0).get("content");
            var parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return "(no parts)";
            Object text = parts.get(0).get("text");
            return (text != null) ? text.toString() : "(empty)";
        } catch (Exception e) {
            return res.toString();
        }
    }

    public String generateFromText(String prompt) {
        return generateFromText(prompt, false); // ✅ 기본값 false로 변경
    }

    public String generateFromText(String prompt, boolean prependDefault) {
        String instruction;
        if (prependDefault) {
            instruction = (prompt == null || prompt.isBlank())
                    ? DEFAULT_ANALYSIS_PROMPT
                    : DEFAULT_ANALYSIS_PROMPT + "\n\n" + prompt;
        } else {
            instruction = (prompt == null) ? "" : prompt;
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", instruction)
                        ))
                )
        );

        Map<?, ?> res = http.post()
                .uri(apiUrl + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).map(bodyText ->
                                new RuntimeException("Gemini POST failed " + resp.statusCode() + ": " + bodyText)))
                .bodyToMono(Map.class)
                .block();

        return extractText(res);
    }

}
