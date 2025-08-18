// spl.reborn.study.entity.UserStudy
package spl.reborn.study.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import spl.reborn.user.entity.User;

import java.time.LocalDate;

@Entity
@Table(name = "user_study")
@Getter @Setter
public class UserStudy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;                // 권장: 연관관계 사용

    private String contentTitle;

    private LocalDate studyDate;

    // ★ Problem의 이미지 URL을 저장
    @Column(name = "image_url", length = 1024)
    private String imageUrl;
}
