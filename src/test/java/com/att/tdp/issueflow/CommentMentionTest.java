package com.att.tdp.issueflow;

import com.att.tdp.issueflow.ticket.TicketEscalationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Checks: @mention parsing on comment create/update, unknown username ignored,
 * duplicate mention de-duplication, and /users/{id}/mentions endpoint.
 */
class CommentMentionTest extends AbstractIntegrationTest {

    @MockitoBean
    TicketEscalationScheduler schedulerMock;

    private String authorToken;
    private Long ticketId;
    private Long dev2Id;

    @BeforeEach
    void setup() throws Exception {
        authorToken = registerAndLogin("author", "DEVELOPER");
        registerAndLogin("dev2", "DEVELOPER");
        dev2Id = getUserId(login("dev2", "pass123"));
        Long projectId = createProject(authorToken);
        ticketId = createTicket(authorToken, projectId, "LOW");
    }

    private String postComment(String content) throws Exception {
        return mockMvc.perform(post("/tickets/" + ticketId + "/comments")
            .header("Authorization", auth(authorToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("content", content))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    }

    @Test
    void mention_knownUser_appearsInMentionedUsers() throws Exception {
        postComment("Hey @dev2 please review this");

        mockMvc.perform(get("/users/" + dev2Id + "/mentions")
            .header("Authorization", auth(authorToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].content").value("Hey @dev2 please review this"));
    }

    @Test
    void mention_unknownUsername_silentlyIgnored() throws Exception {
        String resp = postComment("Hey @nobody fix this");

        // no error — comment created successfully
        org.junit.jupiter.api.Assertions.assertFalse(
            objectMapper.readTree(resp).get("id").isNull());

        // mentioned_users list is empty
        mockMvc.perform(get("/tickets/" + ticketId + "/comments")
            .header("Authorization", auth(authorToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].mentioned_users").isEmpty());
    }

    @Test
    void mention_multipleMentions_allRecorded() throws Exception {
        String authorId2Token = registerAndLogin("dev3", "DEVELOPER");
        Long dev3Id = getUserId(authorId2Token);

        postComment("@dev2 and @dev3 please check this");

        // dev2 sees the mention
        mockMvc.perform(get("/users/" + dev2Id + "/mentions")
            .header("Authorization", auth(authorToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        // dev3 also sees the mention
        mockMvc.perform(get("/users/" + dev3Id + "/mentions")
            .header("Authorization", auth(authorToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void mention_duplicateMention_recordedOnce() throws Exception {
        postComment("@dev2 @dev2 double mention");

        mockMvc.perform(get("/users/" + dev2Id + "/mentions")
            .header("Authorization", auth(authorToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateComment_reparsesMentions() throws Exception {
        String resp = postComment("No mention here");
        Long commentId = objectMapper.readTree(resp).get("id").asLong();

        // update adds a mention
        mockMvc.perform(patch("/tickets/" + ticketId + "/comments/" + commentId)
            .header("Authorization", auth(authorToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("content", "Now mentioning @dev2"))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + dev2Id + "/mentions")
            .header("Authorization", auth(authorToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }
}
