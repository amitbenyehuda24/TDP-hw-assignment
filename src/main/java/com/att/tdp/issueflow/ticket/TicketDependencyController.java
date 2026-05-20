package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.ticket.dto.AddDependencyRequest;
import com.att.tdp.issueflow.ticket.dto.DependencyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/dependencies")
@RequiredArgsConstructor
public class TicketDependencyController {

    private final TicketDependencyService dependencyService;

    @PostMapping
    public ResponseEntity<DependencyResponse> add(
            @PathVariable Long ticketId,
            @Valid @RequestBody AddDependencyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(dependencyService.addDependency(ticketId, req));
    }

    @GetMapping
    public List<DependencyResponse> getAll(@PathVariable Long ticketId) {
        return dependencyService.getDependencies(ticketId);
    }

    @DeleteMapping("/{blockerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long ticketId, @PathVariable Long blockerId) {
        dependencyService.removeDependency(ticketId, blockerId);
    }
}
