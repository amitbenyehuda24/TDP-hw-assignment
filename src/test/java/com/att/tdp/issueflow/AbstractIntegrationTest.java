package com.att.tdp.issueflow;

import com.att.tdp.issueflow.audit.AuditLogRepository;
import com.att.tdp.issueflow.attachment.AttachmentRepository;
import com.att.tdp.issueflow.auth.TokenBlocklistRepository;
import com.att.tdp.issueflow.comment.CommentMentionRepository;
import com.att.tdp.issueflow.comment.CommentRepository;
import com.att.tdp.issueflow.project.ProjectRepository;
import com.att.tdp.issueflow.ticket.TicketDependencyRepository;
import com.att.tdp.issueflow.ticket.TicketRepository;
import com.att.tdp.issueflow.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @Autowired protected UserRepository userRepository;
    @Autowired protected ProjectRepository projectRepository;
    @Autowired protected TicketRepository ticketRepository;
    @Autowired protected CommentRepository commentRepository;
    @Autowired protected CommentMentionRepository commentMentionRepository;
    @Autowired protected TicketDependencyRepository ticketDependencyRepository;
    @Autowired protected AttachmentRepository attachmentRepository;
    @Autowired protected AuditLogRepository auditLogRepository;
    @Autowired protected TokenBlocklistRepository tokenBlocklistRepository;

    @BeforeEach
    void cleanDb() {
        tokenBlocklistRepository.deleteAll();
        commentMentionRepository.deleteAll();
        commentRepository.deleteAll();
        attachmentRepository.deleteAll();
        ticketDependencyRepository.deleteAll();
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String registerAndLogin(String username, String role) throws Exception {
        mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", username + "@test.com",
                "full_name", "Test " + username,
                "role", role,
                "password", "pass123"
            ))));
        return login(username, "pass123");
    }

    protected String login(String username, String password) throws Exception {
        String resp = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("username", username, "password", password)
            )))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("token").asText();
    }

    protected Long getUserId(String token) throws Exception {
        String resp = mockMvc.perform(get("/auth/me")
            .header("Authorization", "Bearer " + token))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("id").asLong();
    }

    protected Long createProject(String token) throws Exception {
        Long ownerId = getUserId(token);
        String resp = mockMvc.perform(post("/projects")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "name", "Test Project",
                "description", "desc",
                "owner_id", ownerId
            ))))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("id").asLong();
    }

    protected Long createTicket(String token, Long projectId, String priority) throws Exception {
        String resp = mockMvc.perform(post("/tickets")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "title", "Test Ticket",
                "priority", priority,
                "type", "BUG",
                "project_id", projectId
            ))))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("id").asLong();
    }

    protected String auth(String token) {
        return "Bearer " + token;
    }
}
