package com.att.tdp.issueflow.comment.dto;

import com.att.tdp.issueflow.user.User;
import lombok.Getter;

@Getter
public class MentionedUserDto {

    private final Long id;
    private final String username;
    private final String fullName;

    public MentionedUserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
    }
}
