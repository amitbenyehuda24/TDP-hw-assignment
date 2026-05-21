package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByAssigneeIdAndProjectIdAndStatusNotAndDeletedAtIsNull(
            Long assigneeId, Long projectId, TicketStatus status);

    @Query("SELECT DISTINCT t.assignee FROM Ticket t " +
           "WHERE t.project.id = :projectId AND t.deletedAt IS NULL AND t.assignee IS NOT NULL")
    List<User> findDistinctAssigneesByProjectId(@Param("projectId") Long projectId);
}
