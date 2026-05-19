package com.att.tdp.issueflow.project.dto;

import com.att.tdp.issueflow.project.Project;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class ProjectResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final Long ownerId;
    private final OffsetDateTime createdAt;

    public ProjectResponse(Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.description = project.getDescription();
        this.ownerId = project.getOwner().getId();
        this.createdAt = project.getCreatedAt();
    }
}
