package com.att.tdp.issueflow.audit.dto;

import com.att.tdp.issueflow.audit.AuditLog;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class AuditLogResponse {

    private final Long id;
    private final String actor;
    private final String action;
    private final String entityType;
    private final Long entityId;

    @JsonRawValue
    private final String oldValue;

    @JsonRawValue
    private final String newValue;

    private final OffsetDateTime createdAt;

    public AuditLogResponse(AuditLog log) {
        this.id = log.getId();
        this.actor = log.getActor();
        this.action = log.getAction();
        this.entityType = log.getEntityType();
        this.entityId = log.getEntityId();
        this.oldValue = log.getOldValue();
        this.newValue = log.getNewValue();
        this.createdAt = log.getCreatedAt();
    }
}
