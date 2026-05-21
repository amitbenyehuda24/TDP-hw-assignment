package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.common.exception.BadRequestException;
import com.att.tdp.issueflow.common.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.project.ProjectService;
import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.WorkloadResponse;
import com.att.tdp.issueflow.user.Role;
import com.att.tdp.issueflow.user.User;
import com.att.tdp.issueflow.user.UserRepository;
import com.att.tdp.issueflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final TicketDependencyService dependencyService;

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
        } else {
            User autoAssigned = autoAssign(req.getProjectId());
            ticket.setAssignee(autoAssigned);
        }

        ticket.setDueDate(req.getDueDate());
        Ticket saved = ticketRepository.save(ticket);
        TicketResponse response = new TicketResponse(saved);
        auditLogService.log("TICKET", "CREATE", response.getId(), null, response);

        if (req.getAssigneeId() == null && saved.getAssignee() != null) {
            auditLogService.log("TICKET", "AUTO_ASSIGN", saved.getId(), null, response, "SYSTEM");
        }

        return response;
    }

    public List<WorkloadResponse> getWorkload(Long projectId) {
        projectService.getOrThrow(projectId);
        return ticketRepository.findDistinctAssigneesByProjectId(projectId)
            .stream()
            .filter(u -> u.getRole() == Role.DEVELOPER)
            .map(u -> new WorkloadResponse(
                u.getId(),
                u.getUsername(),
                ticketRepository.countByAssigneeIdAndProjectIdAndStatusNotAndDeletedAtIsNull(
                    u.getId(), projectId, TicketStatus.DONE)
            ))
            .sorted(Comparator.comparingLong(WorkloadResponse::getOpenTicketCount))
            .toList();
    }

    private User autoAssign(Long projectId) {
        List<User> developers = userRepository.findAllByRole(Role.DEVELOPER);
        if (developers.isEmpty()) return null;
        return developers.stream()
            .min(Comparator
                .comparingLong((User u) ->
                    ticketRepository.countByAssigneeIdAndProjectIdAndStatusNotAndDeletedAtIsNull(
                        u.getId(), projectId, TicketStatus.DONE))
                .thenComparingLong(User::getId))
            .orElse(null);
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

        TicketResponse oldState = new TicketResponse(ticket);

        if (req.getStatus() != null && req.getStatus() != ticket.getStatus()) {
            if (!ticket.getStatus().canTransitionTo(req.getStatus())) {
                throw new BadRequestException(
                    "Invalid status transition: " + ticket.getStatus() + " -> " + req.getStatus()
                );
            }
            if (req.getStatus() == TicketStatus.DONE && dependencyService.hasUnresolvedBlockers(id)) {
                throw new BadRequestException("Cannot mark ticket as DONE: it has unresolved blockers");
            }
            ticket.setStatus(req.getStatus());
        }

        if (req.getTitle() != null)       ticket.setTitle(req.getTitle());
        if (req.getDescription() != null) ticket.setDescription(req.getDescription());
        if (req.getPriority() != null) {
            ticket.setPriority(req.getPriority());
            ticket.setOverdue(false); // manual change resets auto-escalation state
        }
        if (req.getType() != null)        ticket.setType(req.getType());
        if (req.getDueDate() != null)     ticket.setDueDate(req.getDueDate());
        if (req.getAssigneeId() != null)  ticket.setAssignee(userService.getOrThrow(req.getAssigneeId()));

        TicketResponse newState = new TicketResponse(ticketRepository.save(ticket));
        auditLogService.log("TICKET", "UPDATE", id, oldState, newState);
        return newState;
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = getOrThrow(id);
        TicketResponse oldState = new TicketResponse(ticket);
        ticket.setDeletedAt(OffsetDateTime.now());
        ticketRepository.save(ticket);
        auditLogService.log("TICKET", "DELETE", id, oldState, null);
    }

    public Ticket getOrThrow(Long id) {
        return ticketRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    public List<TicketResponse> findDeleted(Long projectId) {
        return ticketRepository.findAllByProjectIdAndDeletedAtIsNotNull(projectId)
            .stream().map(TicketResponse::new).toList();
    }

    @Transactional
    public TicketResponse restoreTicket(Long id) {
        Ticket ticket = ticketRepository.findByIdAndDeletedAtIsNotNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Deleted ticket not found with id: " + id));
        ticket.setDeletedAt(null);
        TicketResponse restored = new TicketResponse(ticketRepository.save(ticket));
        auditLogService.log("TICKET", "RESTORE", id, null, restored);
        return restored;
    }
}
