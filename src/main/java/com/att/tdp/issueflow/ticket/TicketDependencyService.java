package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.common.exception.BadRequestException;
import com.att.tdp.issueflow.common.exception.ConflictException;
import com.att.tdp.issueflow.common.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.ticket.dto.AddDependencyRequest;
import com.att.tdp.issueflow.ticket.dto.DependencyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketDependencyService {

    private final TicketDependencyRepository dependencyRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    private Ticket getOrThrow(Long id) {
        return ticketRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    @Transactional
    public DependencyResponse addDependency(Long ticketId, AddDependencyRequest req) {
        Ticket ticket  = getOrThrow(ticketId);
        Ticket blocker = getOrThrow(req.getBlockedBy());

        if (ticket.getId().equals(blocker.getId())) {
            throw new BadRequestException("A ticket cannot depend on itself");
        }

        if (!ticket.getProject().getId().equals(blocker.getProject().getId())) {
            throw new BadRequestException("Both tickets must belong to the same project");
        }

        if (dependencyRepository.existsByTicketIdAndBlockerId(ticketId, blocker.getId())) {
            throw new ConflictException("Dependency already exists");
        }

        TicketDependency dep = new TicketDependency();
        dep.setTicket(ticket);
        dep.setBlocker(blocker);
        dependencyRepository.save(dep);

        DependencyResponse response = new DependencyResponse(blocker);
        auditLogService.log("DEPENDENCY", "CREATE", dep.getId(), null, response);
        return response;
    }

    public List<DependencyResponse> getDependencies(Long ticketId) {
        getOrThrow(ticketId);
        return dependencyRepository.findAllByTicketId(ticketId)
            .stream().map(d -> new DependencyResponse(d.getBlocker())).toList();
    }

    @Transactional
    public void removeDependency(Long ticketId, Long blockerId) {
        TicketDependency dep = dependencyRepository
            .findByTicketIdAndBlockerId(ticketId, blockerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Dependency not found between ticket " + ticketId + " and blocker " + blockerId));
        DependencyResponse snapshot = new DependencyResponse(dep.getBlocker());
        dependencyRepository.delete(dep);
        auditLogService.log("DEPENDENCY", "DELETE", dep.getId(), snapshot, null);
    }

    public boolean hasUnresolvedBlockers(Long ticketId) {
        return dependencyRepository.existsByTicketIdAndBlockerStatusNot(ticketId, TicketStatus.DONE);
    }
}
