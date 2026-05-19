package com.att.tdp.issueflow.project;

import com.att.tdp.issueflow.common.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.project.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;

    public List<ProjectResponse> findAll() {
        return projectRepository.findAllByDeletedAtIsNull()
            .stream().map(ProjectResponse::new).toList();
    }

    public ProjectResponse findById(Long id) {
        return new ProjectResponse(getOrThrow(id));
    }

    @Transactional
    public ProjectResponse updateProject(Long id, UpdateProjectRequest req) {
        Project project = getOrThrow(id);
        if (req.getName() != null) project.setName(req.getName());
        if (req.getDescription() != null) project.setDescription(req.getDescription());
        return new ProjectResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest req) {
        Project project = new Project();
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project.setOwner(userService.getOrThrow(req.getOwnerId()));
        return new ProjectResponse(projectRepository.save(project));
    }

    public Project getOrThrow(Long id) {
        return projectRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }
}
