package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.common.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.project.ProjectService;
import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ProjectService projectService;
    private final UserService userService;

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest req) {
        Ticket ticket = new Ticket();
        ticket.setTitle(req.getTitle());
        ticket.setDescription(req.getDescription());
        ticket.setPriority(req.getPriority());
        ticket.setType(req.getType());
        ticket.setProject(projectService.getOrThrow(req.getProjectId()));
        if (req.getAssigneeId() != null) {
            ticket.setAssignee(userService.getOrThrow(req.getAssigneeId()));
        }
        ticket.setDueDate(req.getDueDate());
        return new TicketResponse(ticketRepository.save(ticket));
    }

    public Ticket getOrThrow(Long id) {
        return ticketRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }
}
