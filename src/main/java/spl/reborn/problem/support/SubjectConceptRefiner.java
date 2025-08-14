package spl.reborn.problem.support;

import spl.reborn.ai.service.GeminiInlineService;

public final class SubjectConceptRefiner {

    private SubjectConceptRefiner(){}

    public static String buildPrompt(String raw) {
        return """
다음 텍스트에서 문제의 과목(subject)과 핵심 개념(main_concept)만 추출해 JSON으로만 출력하라.
- subject 예시: "수학", "과학", "국어", "영어", "사회", "기타"
- main_concept 예시: "미분", "벡터 내적", "평균속도", "문학 작품 해석"
- 절대 코드블록 없이 순수 JSON만

텍스트:
""" + raw;
    }

    /** Gemini에게 재요청해 JSON만 받기 */
    public static String refine(GeminiInlineService gemini, String raw) {
        return gemini.generateFromText(buildPrompt(raw));
    }
}
