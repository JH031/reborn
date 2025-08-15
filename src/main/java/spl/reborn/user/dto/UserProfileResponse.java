package spl.reborn.user.dto;

import lombok.Builder;
import lombok.Getter;
import spl.reborn.user.entity.SchoolType;

@Getter
@Builder
public class UserProfileResponse {
    private long id;
    private String name;
    private String userid;
    private String email;
    private int grade;
    private SchoolType school;
    private boolean receiveReminders;
}