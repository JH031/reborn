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

    private Long contentId;          // 어떤 학습인지
    private String contentTitle;


    private int offsetDays;
    private LocalDateTime dueAt;
    private boolean sent = false;
}
