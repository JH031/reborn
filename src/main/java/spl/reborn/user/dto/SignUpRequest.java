package spl.reborn.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import spl.reborn.user.entity.SchoolType;

@Getter
public class SignUpRequest {

    private String userid;

    @NotBlank
    private String name;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @Email
    private String email;

    @Min(1)
    @Max(6)
    private int grade;

    private SchoolType school;
}
