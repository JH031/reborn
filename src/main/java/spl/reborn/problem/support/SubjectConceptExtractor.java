package spl.reborn.problem.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SubjectConceptExtractor {

    public record Result(String subject, String mainConcept) {}

    public static Result extract(String raw, ObjectMapper om) {
        if (raw == null || raw.isBlank()) return null;

        String cleaned = stripCodeFence(raw);
        String jsonLike = tryExtractJson(cleaned);
        if (jsonLike == null) jsonLike = cleaned;

        try {
            JsonNode root = om.readTree(jsonLike);

            String subject = getText(root, "subject");

            String mainConcept = null;
            JsonNode needed = root.get("needed_concepts");
            if (needed != null && needed.isArray() && needed.size() > 0) {
                String cand = needed.get(0).asText(null);
                if (notBlank(cand)) mainConcept = cand;
            }

            if (mainConcept == null) {
                String cand = getText(root, "main_concept");
                if (!notBlank(cand)) cand = getText(root, "mainConcept");
                if (notBlank(cand)) mainConcept = cand;
            }

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

    private static String pickKeyword(String summary) {
        String[] tokens = summary.replaceAll("[^가-힣a-zA-Z0-9 ]", " ").split("\\s+");
        for (String t : tokens) {
            if (t.length() >= 2) return t;
        }
        return null;
    }
}
