package spl.reborn.problem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analysisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false)
    private int turn;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_option", length = 32, nullable = true)
    private AnalysisOption option;

    @Column(columnDefinition = "TEXT")
    private String userRequest;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String geminiResponse; // Gemini의 답변

    @Enumerated(EnumType.STRING)
    @Column(name = "similar_option")
    private SimilarOption similarOption;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}