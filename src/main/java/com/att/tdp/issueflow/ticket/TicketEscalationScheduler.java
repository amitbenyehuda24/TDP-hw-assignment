package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketEscalationScheduler {

    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void escalateOverdueTickets() {
        List<Ticket> overdue = ticketRepository
            .findAllByDueDateBeforeAndDeletedAtIsNullAndStatusNot(
                OffsetDateTime.now(), TicketStatus.DONE);

        for (Ticket ticket : overdue) {
            TicketResponse oldState = new TicketResponse(ticket);
            escalate(ticket);
            ticketRepository.save(ticket);
            auditLogService.log("TICKET", "ESCALATE", ticket.getId(), oldState, new TicketResponse(ticket));
        }
    }

    private void escalate(Ticket ticket) {
        switch (ticket.getPriority()) {
            case LOW    -> ticket.setPriority(TicketPriority.MEDIUM);
            case MEDIUM -> ticket.setPriority(TicketPriority.HIGH);
            case HIGH   -> { ticket.setPriority(TicketPriority.CRITICAL); ticket.setOverdue(true); }
            case CRITICAL -> ticket.setOverdue(true);
        }
    }
}
