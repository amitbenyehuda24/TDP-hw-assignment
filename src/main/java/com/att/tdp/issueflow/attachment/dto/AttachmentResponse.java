package com.att.tdp.issueflow.attachment.dto;

import com.att.tdp.issueflow.attachment.Attachment;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class AttachmentResponse {

    private final Long id;
    private final Long ticketId;
    private final Long uploadedBy;
    private final String filename;
    private final String contentType;
    private final Long fileSize;
    private final OffsetDateTime createdAt;

    public AttachmentResponse(Attachment attachment) {
        this.id = attachment.getId();
        this.ticketId = attachment.getTicket().getId();
        this.uploadedBy = attachment.getUploadedBy().getId();
        this.filename = attachment.getFilename();
        this.contentType = attachment.getContentType();
        this.fileSize = attachment.getFileSize();
        this.createdAt = attachment.getCreatedAt();
    }
}
