package com.att.tdp.issueflow.project;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.common.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.project.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public List<ProjectResponse> findAll() {
        return projectRepository.findAllByDeletedAtIsNull()
            .stream().map(ProjectResponse::new).toList();
    }

    public ProjectResponse findById(Long id) {
        return new ProjectResponse(getOrThrow(id));
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest req) {
        Project project = new Project();
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project.setOwner(userService.getOrThrow(req.getOwnerId()));
        ProjectResponse response = new ProjectResponse(projectRepository.save(project));
        auditLogService.log("PROJECT", "CREATE", response.getId(), null, response);
        return response;
    }

    @Transactional
    public ProjectResponse updateProject(Long id, UpdateProjectRequest req) {
        Project project = getOrThrow(id);
        ProjectResponse oldState = new ProjectResponse(project);
        if (req.getName() != null) project.setName(req.getName());
        if (req.getDescription() != null) project.setDescription(req.getDescription());
        ProjectResponse newState = new ProjectResponse(projectRepository.save(project));
        auditLogService.log("PROJECT", "UPDATE", id, oldState, newState);
        return newState;
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = getOrThrow(id);
        ProjectResponse oldState = new ProjectResponse(project);
        project.setDeletedAt(OffsetDateTime.now());
        projectRepository.save(project);
        auditLogService.log("PROJECT", "DELETE", id, oldState, null);
    }

    public Project getOrThrow(Long id) {
        return projectRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }
}
