package com.att.tdp.issueflow.ticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddDependencyRequest {

    @NotNull(message = "blocked_by is required")
    private Long blockedBy;
}
