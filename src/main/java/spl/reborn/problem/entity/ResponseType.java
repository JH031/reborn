package spl.reborn.problem.entity;

public enum ResponseType {
    ANALYSIS,           // 최초 분석 결과 (display 객체 사용)
    SIMILAR_PROBLEMS,   // 유사 문제 (similarProblems 배열 사용)
    TEXT                // 일반 후속 텍스트 답변 (message 필드 사용)
}