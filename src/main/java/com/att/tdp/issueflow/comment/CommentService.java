package com.att.tdp.issueflow.comment;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.comment.dto.CreateCommentRequest;
import com.att.tdp.issueflow.comment.dto.MentionedUserDto;
import com.att.tdp.issueflow.comment.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.common.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.ticket.TicketService;
import com.att.tdp.issueflow.user.UserRepository;
import com.att.tdp.issueflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    private final CommentRepository commentRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final TicketService ticketService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public CommentResponse createComment(Long ticketId, CreateCommentRequest req, Long authorId) {
        Comment comment = new Comment();
        comment.setTicket(ticketService.getOrThrow(ticketId));
        comment.setAuthor(userService.getOrThrow(authorId));
        comment.setContent(req.getContent());
        comment = commentRepository.save(comment);
        syncMentions(comment, req.getContent());
        CommentResponse response = buildResponse(comment);
        auditLogService.log("COMMENT", "CREATE", comment.getId(), null, response);
        return response;
    }

    public List<CommentResponse> findAllByTicket(Long ticketId) {
        ticketService.getOrThrow(ticketId);
        return commentRepository.findAllByTicketId(ticketId)
            .stream().map(this::buildResponse).toList();
    }

    @Transactional
    public CommentResponse updateComment(Long ticketId, Long commentId, UpdateCommentRequest req) {
        Comment comment = getOrThrow(ticketId, commentId);
        CommentResponse oldState = buildResponse(comment);
        comment.setContent(req.getContent());
        comment = commentRepository.save(comment);
        syncMentions(comment, req.getContent());
        CommentResponse newState = buildResponse(comment);
        auditLogService.log("COMMENT", "UPDATE", commentId, oldState, newState);
        return newState;
    }

    @Transactional
    public void deleteComment(Long ticketId, Long commentId) {
        Comment comment = getOrThrow(ticketId, commentId);
        CommentResponse oldState = buildResponse(comment);
        commentMentionRepository.deleteAllByCommentId(commentId);
        commentRepository.delete(comment);
        auditLogService.log("COMMENT", "DELETE", commentId, oldState, null);
    }

    private Comment getOrThrow(Long ticketId, Long commentId) {
        return commentRepository.findByIdAndTicketId(commentId, ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
    }

    // Delete all existing mentions for this comment, then re-parse and insert the current ones.
    // Unknown @usernames are silently ignored — a typo in a mention is not an error.
    private void syncMentions(Comment comment, String content) {
        commentMentionRepository.deleteAllByCommentId(comment.getId());

        Matcher matcher = MENTION_PATTERN.matcher(content);
        Set<String> seen = new HashSet<>();

        while (matcher.find()) {
            String username = matcher.group(1);
            if (!seen.add(username)) continue;

            userRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
                CommentMention mention = new CommentMention();
                mention.setComment(comment);
                mention.setMentionedUser(user);
                commentMentionRepository.save(mention);
            });
        }
    }

    private CommentResponse buildResponse(Comment comment) {
        List<MentionedUserDto> mentionedUsers = commentMentionRepository.findAllByCommentId(comment.getId())
            .stream()
            .map(m -> new MentionedUserDto(m.getMentionedUser()))
            .toList();
        return new CommentResponse(comment, mentionedUsers);
    }
}
