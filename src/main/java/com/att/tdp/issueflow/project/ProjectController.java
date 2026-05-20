package com.att.tdp.issueflow.project;

import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.project.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.ticket.TicketService;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final TicketService ticketService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(req));
    }

    @GetMapping
    public List<ProjectResponse> findAll() {
        return projectService.findAll();
    }

    @GetMapping("/{id}")
    public ProjectResponse findById(@PathVariable Long id) {
        return projectService.findById(id);
    }

    @GetMapping("/{id}/tickets")
    public List<TicketResponse> findTickets(@PathVariable Long id) {
        return ticketService.findAllByProject(id);
    }

    @PatchMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProjectRequest req) {
        return projectService.updateProject(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    @GetMapping("/deleted")
    public List<ProjectResponse> findDeleted(Authentication authentication) {
        userService.requireAdmin((Long) authentication.getPrincipal());
        return projectService.findDeleted();
    }

    @PostMapping("/{id}/restore")
    public ProjectResponse restore(@PathVariable Long id, Authentication authentication) {
        userService.requireAdmin((Long) authentication.getPrincipal());
        return projectService.restoreProject(id);
    }
}
