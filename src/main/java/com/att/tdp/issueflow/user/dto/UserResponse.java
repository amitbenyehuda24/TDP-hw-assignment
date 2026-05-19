package com.att.tdp.issueflow.user.dto;

import com.att.tdp.issueflow.user.Role;
import com.att.tdp.issueflow.user.User;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class UserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final String fullName;
    private final Role role;
    private final OffsetDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
    }
}
