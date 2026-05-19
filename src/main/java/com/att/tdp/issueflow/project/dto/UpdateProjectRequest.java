package com.att.tdp.issueflow.project.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectRequest {

    @Size(min = 1, max = 255, message = "name must be between 1 and 255 characters")
    private String name;

    private String description;
}
