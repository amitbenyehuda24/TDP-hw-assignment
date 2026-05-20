package com.att.tdp.issueflow.attachment;

import com.att.tdp.issueflow.attachment.dto.AttachmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Long uploaderId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(attachmentService.upload(ticketId, file, uploaderId));
    }

    @GetMapping
    public List<AttachmentResponse> listMetadata(@PathVariable Long ticketId) {
        return attachmentService.findAllByTicket(ticketId);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> download(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId) {
        Attachment attachment = attachmentService.getFileOrThrow(ticketId, attachmentId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + attachment.getFilename() + "\"")
            .contentType(MediaType.parseMediaType(attachment.getContentType()))
            .body(attachment.getFileData());
    }
}
