// spl.reborn.study.entity.ReviewProgress.java
package spl.reborn.study.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import spl.reborn.user.entity.User;

import java.time.LocalDate;

@Entity
@Getter @Setter
public class ReviewProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private UserStudy userStudy;

    // 🔁 필드명 'user' → 'owner'로 변경 (확실히 User 객체로 매핑)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id") // 컬럼명 명시 (권장)
    private User owner;

    private int stageIndex;
    private LocalDate nextReviewDate;
    private boolean completed;

    @Column(length = 1024)   // S3 URL 길이 대비
    private String imageUrl;  // ★ 추가

    @Enumerated(EnumType.STRING)
    private UnderstandingResult lastResult;

    public enum UnderstandingResult { UNDERSTOOD, NOT_UNDERSTOOD }
}
