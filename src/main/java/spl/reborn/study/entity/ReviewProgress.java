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


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User owner;

    private int stageIndex;
    private LocalDate nextReviewDate;
    private boolean completed;

    @Column(length = 1024)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private UnderstandingResult lastResult;

    public enum UnderstandingResult { UNDERSTOOD, NOT_UNDERSTOOD }
}
