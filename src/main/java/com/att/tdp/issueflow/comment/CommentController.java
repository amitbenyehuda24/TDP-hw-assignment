package com.att.tdp.issueflow.comment;

import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.comment.dto.CreateCommentRequest;
import com.att.tdp.issueflow.comment.dto.UpdateCommentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateCommentRequest req,
            Authentication authentication) {
        Long authorId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(commentService.createComment(ticketId, req, authorId));
    }

    @GetMapping
    public List<CommentResponse> findAll(@PathVariable Long ticketId) {
        return commentService.findAllByTicket(ticketId);
    }

    @PatchMapping("/{commentId}")
    public CommentResponse update(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest req) {
        return commentService.updateComment(ticketId, commentId, req);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long ticketId, @PathVariable Long commentId) {
        commentService.deleteComment(ticketId, commentId);
    }
}
