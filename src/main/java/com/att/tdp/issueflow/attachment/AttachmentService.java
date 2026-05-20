package com.att.tdp.issueflow.attachment;

import com.att.tdp.issueflow.attachment.dto.AttachmentResponse;
import com.att.tdp.issueflow.common.exception.BadRequestException;
import com.att.tdp.issueflow.common.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.ticket.TicketService;
import com.att.tdp.issueflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final Set<String> ALLOWED_TYPES =
        Set.of("image/png", "image/jpeg", "application/pdf", "text/plain");

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final AttachmentRepository attachmentRepository;
    private final TicketService ticketService;
    private final UserService userService;

    @Transactional
    public AttachmentResponse upload(Long ticketId, MultipartFile file, Long uploaderId) {
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("File exceeds the 10 MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException(
                "File type not allowed. Permitted: image/png, image/jpeg, application/pdf, text/plain"
            );
        }

        Attachment attachment = new Attachment();
        attachment.setTicket(ticketService.getOrThrow(ticketId));
        attachment.setUploadedBy(userService.getOrThrow(uploaderId));
        attachment.setFilename(file.getOriginalFilename());
        attachment.setContentType(contentType);
        attachment.setFileSize(file.getSize());

        try {
            attachment.setFileData(file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded file");
        }

        return new AttachmentResponse(attachmentRepository.save(attachment));
    }

    public List<AttachmentResponse> findAllByTicket(Long ticketId) {
        ticketService.getOrThrow(ticketId);
        return attachmentRepository.findAllByTicketId(ticketId)
            .stream().map(AttachmentResponse::new).toList();
    }

    public Attachment getFileOrThrow(Long ticketId, Long attachmentId) {
        return attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Attachment not found with id: " + attachmentId));
    }
}
