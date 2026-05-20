package com.att.tdp.issueflow.ticket;

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

        return new DependencyResponse(blocker);
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
        dependencyRepository.delete(dep);
    }

    public boolean hasUnresolvedBlockers(Long ticketId) {
        return dependencyRepository.existsByTicketIdAndBlockerStatusNot(ticketId, TicketStatus.DONE);
    }
}
