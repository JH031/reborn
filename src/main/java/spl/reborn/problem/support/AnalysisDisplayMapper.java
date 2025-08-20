// spl/reborn/problem/support/AnalysisDisplayMapper.java
package spl.reborn.problem.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import spl.reborn.problem.entity.AnalysisOption;

import java.util.*;

public final class AnalysisDisplayMapper {
    private AnalysisDisplayMapper(){}

    public static Map<String, Object> toDisplay(String raw, AnalysisOption option, ObjectMapper om) {
        if (raw == null || raw.isBlank()) return Map.of("feedback", "분석 결과가 비어 있습니다.");

        String cleaned = stripCodeFence(raw);
        String jsonLike = tryExtractJson(cleaned);
        if (jsonLike == null) jsonLike = cleaned;

        try {
            JsonNode r = om.readTree(jsonLike);
            Map<String, Object> m = switch (option) {
                case APPROACH      -> buildApproach(r);
                case FULL_SOLUTION -> buildFullSolution(r);
                case FIND_MY_ERROR -> buildFindMyError(r);
            };
            m.entrySet().removeIf(e -> isEmpty(e.getValue()));
            if (m.isEmpty()) return Map.of("feedback", text(r, "feedback"));
            return m;
        } catch (Exception e) {
            return Map.of("feedback", "응답 파싱 실패. 원문 일부: " + cleaned.substring(0, Math.min(200, cleaned.length())));
        }
    }

    private static Map<String, Object> buildApproach(JsonNode r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subject", text(r, "subject"));
        m.put("problem_summary", text(r, "problem_summary"));
        m.put("given", listOfText(r.get("given")));
        m.put("asked", text(r, "asked"));
        m.put("needed_concepts", listOfText(r.get("needed_concepts")));
        m.put("how_to_approach", text(r, "how_to_approach"));
        m.put("feedback", text(r, "feedback"));
        return m;
    }

    private static Map<String, Object> buildFullSolution(JsonNode r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subject", text(r, "subject"));
        m.put("problem_summary", text(r, "problem_summary"));
        m.put("given", listOfText(r.get("given")));
        m.put("asked", text(r, "asked"));
        m.put("correct_answer", text(r, "correct_answer"));
        m.put("step_by_step_explain", text(r, "step_by_step_explain"));
        m.put("feedback", text(r, "feedback"));
        return m;
    }

    private static Map<String, Object> buildFindMyError(JsonNode r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subject", text(r, "subject"));
        m.put("has_solution", bool(r, "has_solution"));
        m.put("solution_evidence", text(r, "solution_evidence"));
        m.put("is_correct", boolOrNull(r, "is_correct")); // true/false/null
        m.put("error_analysis", errorAnalysis(r.get("error_analysis")));
        m.put("correct_answer", text(r, "correct_answer"));
        m.put("step_by_step_explain", text(r, "step_by_step_explain"));
        m.put("feedback", text(r, "feedback"));
        return m;
    }

    private static String stripCodeFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int first = t.indexOf('\n');
            int last = t.lastIndexOf("```");
            if (first > 0 && last > first) return t.substring(first + 1, last).trim();
        }
        return t;
    }
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
    private static String text(JsonNode r, String key) {
        JsonNode n = r.get(key);
        return (n != null && n.isTextual()) ? n.asText() : null;
    }
    private static Boolean bool(JsonNode r, String key) {
        JsonNode n = r.get(key);
        return (n != null && n.isBoolean()) ? n.asBoolean() : null;
    }
    private static Object boolOrNull(JsonNode r, String key) {
        JsonNode n = r.get(key);
        if (n == null || n.isNull()) return null;
        if (n.isBoolean()) return n.asBoolean();
        if (n.isTextual()) {
            String v = n.asText();
            if ("true".equalsIgnoreCase(v)) return true;
            if ("false".equalsIgnoreCase(v)) return false;
        }
        return null;
    }
    private static List<String> listOfText(JsonNode arr) {
        if (arr == null || !arr.isArray()) return null;
        List<String> out = new ArrayList<>();
        for (JsonNode n : arr) if (n.isTextual()) out.add(n.asText());
        return out.isEmpty() ? null : out;
    }
    private static List<Map<String, Object>> errorAnalysis(JsonNode arr) {
        if (arr == null || !arr.isArray()) return null;
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode n : arr) {
            if (!n.isObject()) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            String type = text(n, "type");
            String explanation = text(n, "explanation");
            if (type != null) item.put("type", type);
            if (explanation != null) item.put("explanation", explanation);
            if (!item.isEmpty()) out.add(item);
        }
        return out.isEmpty() ? null : out;
    }
    private static boolean isEmpty(Object v) {
        if (v == null) return true;
        if (v instanceof String s) return s.isBlank();
        if (v instanceof Collection<?> c) return c.isEmpty();
        if (v instanceof Map<?, ?> m) return m.isEmpty();
        return false;
    }
}
