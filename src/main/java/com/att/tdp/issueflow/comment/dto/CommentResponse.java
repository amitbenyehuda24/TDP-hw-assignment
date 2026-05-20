package com.att.tdp.issueflow.comment.dto;

import com.att.tdp.issueflow.comment.Comment;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class CommentResponse {

    private final Long id;
    private final Long ticketId;
    private final Long authorId;
    private final String content;
    private final List<Long> mentionedUserIds;
    private final Long version;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public CommentResponse(Comment comment, List<Long> mentionedUserIds) {
        this.id = comment.getId();
        this.ticketId = comment.getTicket().getId();
        this.authorId = comment.getAuthor().getId();
        this.content = comment.getContent();
        this.mentionedUserIds = mentionedUserIds;
        this.version = comment.getVersion();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
    }
}
