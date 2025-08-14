package spl.reborn.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import spl.reborn.problem.entity.Problem;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String name;

    @Column(unique = true, nullable = false)
    private String userid;

    private String password;

    @Column(unique = true)
    private String email;

    private int grade;

    @Enumerated(EnumType.STRING)
    private SchoolType school;

    private boolean receiveReminders = false; // 복습 알림을 받기 위한 메일 수신 동의

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Problem> problems = new ArrayList<>();
}
