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

    /** 퍼블릭/프리사인드 S3 URL을 인라인(Base64)로 변환해 Gemini 호출 */
    public String generateFromImageUrl(String imageUrl, String prompt) {
        // 1) 이미지 바이트 다운로드
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
        System.out.println("[GeminiInline] downloaded bytes = " + bytes.length);

        // 2) MIME 추정
        String mime = guessMime(bytes, imageUrl);
        System.out.println("[GeminiInline] mime = " + mime);

        // 3) Base64 인코딩 (크기 로깅)
        String b64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println("[GeminiInline] base64 length = " + b64.length());

        // 4) 요청 바디 구성
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("inline_data", Map.of(
                                        "mime_type", mime,
                                        "data", b64
                                )),
                                Map.of("text", (prompt == null || prompt.isBlank())
                                        ? "이 이미지를 설명해줘."
                                        : prompt)
                        ))
                )
        );

        // 5) Gemini 호출
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

        // 6) 텍스트 추출 (파싱 실패 시 원문 반환)
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
}
