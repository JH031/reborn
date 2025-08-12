package spl.reborn.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
}
