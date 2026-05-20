package com.att.tdp.issueflow.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketDependencyRepository extends JpaRepository<TicketDependency, Long> {

    List<TicketDependency> findAllByTicketId(Long ticketId);

    Optional<TicketDependency> findByTicketIdAndBlockerId(Long ticketId, Long blockerId);

    boolean existsByTicketIdAndBlockerId(Long ticketId, Long blockerId);

    boolean existsByTicketIdAndBlockerStatusNot(Long ticketId, TicketStatus status);
}
