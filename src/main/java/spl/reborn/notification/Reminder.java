package spl.reborn.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import spl.reborn.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(
        uniqueConstraints = {
                // 같은 user + 같은 contentId + 같은 offsetDays 예약은 1개만
                @UniqueConstraint(columnNames = {"user_id", "contentId", "offsetDays"})
        }
)
@Getter @Setter
public class Reminder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    private Long contentId;          // ★ 어떤 학습(콘텐츠)인지 구분용
    private String contentTitle;     // 메일 제목에 표시

    private int offsetDays;          // 1,3,7,30
    private LocalDateTime dueAt;     // 발송 예정 시각
    private boolean sent = false;
}
