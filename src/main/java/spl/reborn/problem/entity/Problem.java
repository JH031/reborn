package spl.reborn.problem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import spl.reborn.user.entity.User;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long problemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1024) // S3 URL 대비 넉넉히
    private String originalImageUrl;

    @Column(length = 32)
    private String subject;

    @Column(length = 128)
    private String mainConcept;

    private LocalDateTime createdAt = LocalDateTime.now();
}