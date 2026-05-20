package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.common.exception.BadRequestException;
import com.att.tdp.issueflow.common.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.project.ProjectService;
import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

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

    public List<TicketResponse> findAllByProject(Long projectId) {
        projectService.getOrThrow(projectId);
        return ticketRepository.findAllByProjectIdAndDeletedAtIsNull(projectId)
            .stream().map(TicketResponse::new).toList();
    }

    public TicketResponse findById(Long id) {
        return new TicketResponse(getOrThrow(id));
    }

    @Transactional
    public TicketResponse updateTicket(Long id, UpdateTicketRequest req) {
        Ticket ticket = getOrThrow(id);

        if (ticket.getStatus() == TicketStatus.DONE) {
            throw new BadRequestException("Cannot modify a ticket that is already DONE");
        }

        if (req.getStatus() != null && req.getStatus() != ticket.getStatus()) {
            if (!ticket.getStatus().canTransitionTo(req.getStatus())) {
                throw new BadRequestException(
                    "Invalid status transition: " + ticket.getStatus() + " -> " + req.getStatus()
                );
            }
            ticket.setStatus(req.getStatus());
        }

        if (req.getTitle() != null)    ticket.setTitle(req.getTitle());
        if (req.getDescription() != null) ticket.setDescription(req.getDescription());
        if (req.getPriority() != null) ticket.setPriority(req.getPriority());
        if (req.getType() != null)     ticket.setType(req.getType());
        if (req.getDueDate() != null)  ticket.setDueDate(req.getDueDate());
        if (req.getAssigneeId() != null) {
            ticket.setAssignee(userService.getOrThrow(req.getAssigneeId()));
        }

        return new TicketResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = getOrThrow(id);
        ticket.setDeletedAt(OffsetDateTime.now());
        ticketRepository.save(ticket);
    }

    public Ticket getOrThrow(Long id) {
        return ticketRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }
}
