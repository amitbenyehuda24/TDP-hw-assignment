package com.att.tdp.issueflow.user.dto;

import com.att.tdp.issueflow.user.Role;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    // null means "do not update this field"
    @Size(min = 1, max = 255, message = "full_name must be between 1 and 255 characters")
    private String fullName;

    private Role role;
}
