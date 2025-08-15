package spl.reborn.user.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import spl.reborn.user.entity.SchoolType;

@Getter
@AllArgsConstructor
public class TokenResponse {
    private String token;   // JWT 토큰
    private long id;        // User PK (auto increment id)
    private String name;
    private String email;
    private int grade;
    private SchoolType school;
}
