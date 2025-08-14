package spl.reborn.ai.prompt;

public final class AnalysisPrompts {
    private AnalysisPrompts(){}

    public static final String APPROACH_HINT = """
추가 지시:
- 지금은 '문제 접근 방법'만 간결히 안내하라.
- 'needed_concepts'와 'how_to_approach'를 충실히 채워라.
- 정답을 단정 짓지 말고 전략 중심으로 설명하라.
""";

    public static final String FULL_SOLUTION_HINT = """
추가 지시:
- 지금은 '전체 풀이'를 요청한다.
- 'step_by_step_explain'을 구체적으로 작성하고 정답을 명확히 제시하라.
""";

    public static String findMyErrorHint(String userSolution) {
        String safe = userSolution == null ? "" : userSolution;
        return """
추가 지시:
- 지금은 '내 풀이 오류 찾기'를 요청한다.
- 아래 사용자의 풀이를 검토하여 오류 유형과 원인을 분석하라.
- 올바른 핵심 풀이와 정답도 제시하라.

[사용자 풀이]
""" + safe + "\n";
    }

    public static String followUp(String recentSummaries, String userPrompt) {
        return """
역할: 너는 이전 분석 결과를 참고하여 학생의 추가 요청에 답하는 과외 선생님이다.

규칙:
- 가능하면 이전의 JSON 스키마를 유지해 답하되, 사용자의 요청이 자유형 설명/질문이면 'feedback'을 중심으로 간결한 한국어 설명을 제공해도 된다.
- 새로운 정정/추가 풀이가 있으면 반영하라.
- 수식은 간단한 LaTeX 또는 평문.

[이전 분석 요약(최신순 1~2개)]
""" + recentSummaries + """

[사용자 추가 요청]
""" + userPrompt + "\n";
    }
}
