package com.att.tdp.issueflow.ticket.dto;

import com.att.tdp.issueflow.ticket.TicketPriority;
import com.att.tdp.issueflow.ticket.TicketStatus;
import com.att.tdp.issueflow.ticket.TicketType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UpdateTicketRequest {

    @Size(min = 1, max = 255, message = "title must be between 1 and 255 characters")
    private String title;

    private String description;

    private TicketPriority priority;

    private TicketType type;

    private Long assigneeId;

    private OffsetDateTime dueDate;

    private TicketStatus status;
}
