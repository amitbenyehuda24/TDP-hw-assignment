package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.ImportSummaryResponse;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketCsvService ticketCsvService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(req));
    }

    @GetMapping
    public List<TicketResponse> findByProject(@RequestParam Long projectId) {
        return ticketService.findAllByProject(projectId);
    }

    // Literal path segments (/export, /import, /deleted) are matched before path variables (/{id})
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam Long projectId) {
        byte[] csv = ticketCsvService.exportToCsv(projectId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportSummaryResponse importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long projectId) {
        return ticketCsvService.importFromCsv(file, projectId);
    }

    @GetMapping("/{id}")
    public TicketResponse findById(@PathVariable Long id) {
        return ticketService.findById(id);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest req) {
        return ticketService.updateTicket(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ticketService.deleteTicket(id);
    }

    @GetMapping("/deleted")
    public List<TicketResponse> findDeleted(
            @RequestParam Long projectId,
            Authentication authentication) {
        userService.requireAdmin((Long) authentication.getPrincipal());
        return ticketService.findDeleted(projectId);
    }

    @PostMapping("/{id}/restore")
    public TicketResponse restore(@PathVariable Long id, Authentication authentication) {
        userService.requireAdmin((Long) authentication.getPrincipal());
        return ticketService.restoreTicket(id);
    }
}
