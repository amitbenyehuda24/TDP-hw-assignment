package com.att.tdp.issueflow.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findAllByProjectIdAndDeletedAtIsNull(Long projectId);

    Optional<Ticket> findByIdAndDeletedAtIsNull(Long id);

    List<Ticket> findAllByProjectIdAndDeletedAtIsNotNull(Long projectId);

    Optional<Ticket> findByIdAndDeletedAtIsNotNull(Long id);

    List<Ticket> findAllByDueDateBeforeAndDeletedAtIsNullAndStatusNot(
            OffsetDateTime now, TicketStatus status);
}
