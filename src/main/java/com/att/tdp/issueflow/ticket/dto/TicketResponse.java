package com.att.tdp.issueflow.ticket.dto;

import com.att.tdp.issueflow.ticket.Ticket;
import com.att.tdp.issueflow.ticket.TicketPriority;
import com.att.tdp.issueflow.ticket.TicketStatus;
import com.att.tdp.issueflow.ticket.TicketType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class TicketResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final TicketStatus status;
    private final TicketPriority priority;
    private final TicketType type;
    private final Long projectId;
    private final Long assigneeId;
    private final OffsetDateTime dueDate;

    @JsonProperty("is_overdue")
    private final boolean overdue;

    private final Long version;
    private final OffsetDateTime createdAt;

    public TicketResponse(Ticket ticket) {
        this.id = ticket.getId();
        this.title = ticket.getTitle();
        this.description = ticket.getDescription();
        this.status = ticket.getStatus();
        this.priority = ticket.getPriority();
        this.type = ticket.getType();
        this.projectId = ticket.getProject().getId();
        this.assigneeId = ticket.getAssignee() != null ? ticket.getAssignee().getId() : null;
        this.dueDate = ticket.getDueDate();
        this.overdue = ticket.isOverdue();
        this.version = ticket.getVersion();
        this.createdAt = ticket.getCreatedAt();
    }
}
