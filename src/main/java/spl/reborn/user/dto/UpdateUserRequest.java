package spl.reborn.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import spl.reborn.user.entity.SchoolType;

@Getter @Setter
public class UpdateUserRequest {
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @Min(value = 1, message = "grade는 1 이상이어야 합니다.")
    @Max(value = 6, message = "grade는 6 이하여야 합니다.")
    private Integer grade;

    private SchoolType school;
}
