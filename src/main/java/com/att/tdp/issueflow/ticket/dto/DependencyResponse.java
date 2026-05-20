package com.att.tdp.issueflow.ticket.dto;

import com.att.tdp.issueflow.ticket.Ticket;
import com.att.tdp.issueflow.ticket.TicketPriority;
import com.att.tdp.issueflow.ticket.TicketStatus;
import lombok.Getter;

@Getter
public class DependencyResponse {

    private final Long blockerId;
    private final String blockerTitle;
    private final TicketStatus blockerStatus;
    private final TicketPriority blockerPriority;
    private final Long projectId;

    public DependencyResponse(Ticket blocker) {
        this.blockerId = blocker.getId();
        this.blockerTitle = blocker.getTitle();
        this.blockerStatus = blocker.getStatus();
        this.blockerPriority = blocker.getPriority();
        this.projectId = blocker.getProject().getId();
    }
}
