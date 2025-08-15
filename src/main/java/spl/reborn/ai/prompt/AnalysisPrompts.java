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
사용자의 요청은 'FULL_SOLUTION' (전체 풀이) 입니다.
    당신이 이미지에서 사용자의 풀이 과정을 찾았는지 여부('has_solution' 값)와 관계없이, 반드시 'step_by_step_explain' 필드에 상세하고 친절한 단계별 풀이 과정을 작성하여 제공해야 합니다.
    'how_to_approach' 필드에는 간단한 접근법 요약을, 'step_by_step_explain' 필드에는 완전한 풀이를 담아주세요.
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

    public static final String SIMILAR_PROBLEMS_FROM_FULL_ANALYSIS = """
역할: 너는 초/중/고 문제 출제 전문가다.
입력으로 원문 문제에 대한 상세 분석 내용(JSON 형식)을 받는다.

목표:
- 원문과 "핵심 개념·형식·난이도"는 유사하되, 수치/문맥/고유명사는 변경하여 완전히 새로운 2개의 문제를 생성하라.
- 각 문항은 독립적으로 성립해야 하며, 모호하거나 추가 자료 없이 풀 수 있어야 한다.
- 수학 기호는 간단한 LaTeX(예: \\frac, \\sqrt) 허용. 단위는 명시한다.
- 원문 분석 내용 속 풀이방식과 개념을 참고하여 변형하되, 정답·풀이가 일관성 있도록 검증할 것.
- 표절 금지: 원문 문장/수치/사례를 그대로 베끼지 말 것.

출력 형식: 아래의 JSON "만" 출력하라. (코드블록 금지, 추가 설명 금지)
{
  "problems": [
    {
      "question": "…",        // 학생에게 제시할 문제 본문(한국어)
      "answer": "…",          // 최종 정답(숫자/식/문장 모두 허용)
      "solution_steps": "…"   // 핵심 풀이과정 요약(2~6문장)
    },
    {
      "question": "…",
      "answer": "…",
      "solution_steps": "…"
    }
  ]
}

제약:
- problems 길이는 정확히 2.
- 모든 출력은 한국어.
""";

}
