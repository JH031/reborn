package spl.reborn.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponse {
    private String token;   // JWT 토큰
    private long id;        // User PK (auto increment id)
    private String name;
    private String email;
}
