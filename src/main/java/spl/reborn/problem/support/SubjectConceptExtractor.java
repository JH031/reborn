package spl.reborn.problem.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SubjectConceptExtractor {

    private SubjectConceptExtractor(){}

    public record Result(String subject, String mainConcept) {}

    /** Gemini 원문에서 subject/mainConcept 추출 (JSON이 아니어도 최대한 복구) */
    public static Result extract(String raw, ObjectMapper om) {
        if (raw == null || raw.isBlank()) return null;

        String cleaned = stripCodeFence(raw);
        String jsonLike = tryExtractJson(cleaned);
        if (jsonLike == null) jsonLike = cleaned; // 마지막 시도

        try {
            JsonNode root = om.readTree(jsonLike);

            // subject
            String subject = getText(root, "subject");

            // mainConcept 후보 1: needed_concepts[0]
            String mainConcept = null;
            JsonNode needed = root.get("needed_concepts");
            if (needed != null && needed.isArray() && needed.size() > 0) {
                String cand = needed.get(0).asText(null);
                if (notBlank(cand)) mainConcept = cand;
            }

            // 후보 2: main_concept / mainConcept 필드가 있을 수도
            if (mainConcept == null) {
                String cand = getText(root, "main_concept");
                if (!notBlank(cand)) cand = getText(root, "mainConcept");
                if (notBlank(cand)) mainConcept = cand;
            }

            // 후보 3: problem_summary에서 간단 키워드 추출(아주 얕은 휴리스틱)
            if (mainConcept == null) {
                String summary = getText(root, "problem_summary");
                if (notBlank(summary)) {
                    mainConcept = pickKeyword(summary);
                }
            }

            if (!notBlank(subject) && !notBlank(mainConcept)) return null;
            return new Result(blankToNull(subject), blankToNull(mainConcept));
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String stripCodeFence(String s) {
        // ```json ... ``` 제거
        String t = s.trim();
        if (t.startsWith("```")) {
            int first = t.indexOf('\n');
            int last = t.lastIndexOf("```");
            if (first > 0 && last > first) {
                return t.substring(first + 1, last).trim();
            }
        }
        return t;
    }

    /** 문자열에서 가장 바깥 {} JSON 덩어리만 골라내기 */
    private static String tryExtractJson(String s) {
        int start = s.indexOf('{');
        if (start < 0) return null;
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return s.substring(start, i + 1);
            }
        }
        return null;
    }

    private static String getText(JsonNode root, String field) {
        JsonNode n = root.get(field);
        return n != null && n.isTextual() ? n.asText() : null;
    }

    private static boolean notBlank(String s){ return s != null && !s.isBlank(); }
    private static String blankToNull(String s){ return (s == null || s.isBlank()) ? null : s; }

    /** 아주 단순 키워드 추출(한 단어 정도 뽑기) */
    private static String pickKeyword(String summary) {
        // 예: "미분을 이용해 접선을 구하라" -> "미분"
        String[] tokens = summary.replaceAll("[^가-힣a-zA-Z0-9 ]", " ").split("\\s+");
        for (String t : tokens) {
            if (t.length() >= 2) return t; // 너무 단기억용: 최소 2글자
        }
        return null;
    }
}
