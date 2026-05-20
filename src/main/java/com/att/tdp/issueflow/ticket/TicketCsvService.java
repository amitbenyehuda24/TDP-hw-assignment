package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.common.exception.BadRequestException;
import com.att.tdp.issueflow.project.ProjectService;
import com.att.tdp.issueflow.ticket.dto.ImportSummaryResponse;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketCsvService {

    private static final String[] EXPORT_HEADERS =
        {"id", "title", "description", "status", "priority", "type", "assignee_id"};

    private final TicketRepository ticketRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public byte[] exportToCsv(Long projectId) {
        projectService.getOrThrow(projectId);
        List<Ticket> tickets = ticketRepository.findAllByProjectIdAndDeletedAtIsNull(projectId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
             CSVPrinter printer = new CSVPrinter(writer,
                 CSVFormat.RFC4180.builder().setHeader(EXPORT_HEADERS).build())) {

            for (Ticket t : tickets) {
                printer.printRecord(
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    t.getStatus(),
                    t.getPriority(),
                    t.getType(),
                    t.getAssignee() != null ? t.getAssignee().getId() : ""
                );
            }
            printer.flush();
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }

    // Each row is saved via its own JpaRepository transaction — no @Transactional here
    // so a failed row never rolls back a previously saved row.
    public ImportSummaryResponse importFromCsv(MultipartFile file, Long projectId) {
        projectService.getOrThrow(projectId);

        int created = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.RFC4180.builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .build()
                 .parse(reader)) {

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                try {
                    Ticket ticket = buildTicketFromRecord(record, projectId);
                    ticketRepository.save(ticket);
                    auditLogService.log("TICKET", "CREATE", ticket.getId(), null, new TicketResponse(ticket));
                    created++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new BadRequestException("Failed to parse CSV file: " + e.getMessage());
        }

        return new ImportSummaryResponse(created, failed, errors);
    }

    private Ticket buildTicketFromRecord(CSVRecord record, Long projectId) {
        String title = record.get("title");
        if (title == null || title.isBlank()) {
            throw new BadRequestException("title is required");
        }

        Ticket ticket = new Ticket();
        ticket.setTitle(title.trim());
        ticket.setDescription(getOptional(record, "description"));
        ticket.setStatus(parseEnum(TicketStatus.class, record.get("status"), "status"));
        ticket.setPriority(parseEnum(TicketPriority.class, record.get("priority"), "priority"));
        ticket.setType(parseEnum(TicketType.class, record.get("type"), "type"));
        ticket.setProject(projectService.getOrThrow(projectId));

        String assigneeIdStr = getOptional(record, "assignee_id");
        if (assigneeIdStr != null && !assigneeIdStr.isBlank()) {
            ticket.setAssignee(userService.getOrThrow(Long.parseLong(assigneeIdStr.trim())));
        }

        return ticket;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid " + fieldName + " value: " + value.trim());
        }
    }

    private String getOptional(CSVRecord record, String column) {
        try {
            String val = record.get(column);
            return (val == null || val.isBlank()) ? null : val;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
