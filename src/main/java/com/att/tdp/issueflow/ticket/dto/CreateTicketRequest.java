package com.att.tdp.issueflow.ticket.dto;

import com.att.tdp.issueflow.ticket.TicketPriority;
import com.att.tdp.issueflow.ticket.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class CreateTicketRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    private String description;

    @NotNull(message = "priority is required")
    private TicketPriority priority;

    @NotNull(message = "type is required")
    private TicketType type;

    @NotNull(message = "project_id is required")
    private Long projectId;

    private Long assigneeId;

    private OffsetDateTime dueDate;
}
