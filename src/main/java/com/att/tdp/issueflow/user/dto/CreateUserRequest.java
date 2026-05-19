package com.att.tdp.issueflow.user.dto;

import com.att.tdp.issueflow.user.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "username is required")
    @Size(max = 50, message = "username must be at most 50 characters")
    private String username;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "full_name is required")
    @Size(max = 255, message = "full_name must be at most 255 characters")
    private String fullName;

    @NotNull(message = "role is required")
    private Role role;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100, message = "password must be between 6 and 100 characters")
    private String password;
}
