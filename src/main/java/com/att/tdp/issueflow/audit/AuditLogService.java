package com.att.tdp.issueflow.audit;

import com.att.tdp.issueflow.audit.dto.AuditLogResponse;
import com.att.tdp.issueflow.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public void log(String entityType, String action, Long entityId, Object oldValue, Object newValue) {
        AuditLog entry = new AuditLog();
        entry.setActor(resolveActor());
        entry.setEntityType(entityType);
        entry.setAction(action);
        entry.setEntityId(entityId);
        entry.setOldValue(toJson(oldValue));
        entry.setNewValue(toJson(newValue));
        auditLogRepository.save(entry);
    }

    public List<AuditLogResponse> findAll(String actor, String entityType, Long entityId) {
        Specification<AuditLog> spec = Specification.where(null);
        if (actor != null)      spec = spec.and((root, q, cb) -> cb.equal(root.get("actor"), actor));
        if (entityType != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("entityType"), entityType));
        if (entityId != null)   spec = spec.and((root, q, cb) -> cb.equal(root.get("entityId"), entityId));

        return auditLogRepository
            .findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream().map(AuditLogResponse::new).toList();
    }

    // Read the userId stored as principal by JwtFilter, then resolve the username.
    // Returns "SYSTEM" for scheduler-triggered calls that have no request context.
    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) return "SYSTEM";
        return userRepository.findById(userId)
            .map(u -> u.getUsername())
            .orElse("SYSTEM");
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
