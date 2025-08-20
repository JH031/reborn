// spl.reborn.study.entity.StudyCheck
package spl.reborn.study.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import spl.reborn.study.entity.UserStudy;
import spl.reborn.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(
        uniqueConstraints = {
                // 같은 유저 + 같은 학습 + 같은 스테이지(1/4/7/14/30)는 한 번만 체크
                @UniqueConstraint(columnNames = {"user_id", "user_study_id", "stageDay"})
        }
)
public class StudyCheck {

    public enum Result {
        UNDERSTOOD, NOT_UNDERSTOOD
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private UserStudy userStudy;


    @Column(nullable = false)
    private int stageDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Result result;

    @Column(nullable = false)
    private LocalDateTime checkedAt = LocalDateTime.now();
}
