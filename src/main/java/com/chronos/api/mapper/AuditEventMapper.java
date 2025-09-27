package com.chronos.api.mapper;

import com.chronos.api.dto.audit.AuditEventResponse;
import com.chronos.domain.model.AuditEvent;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditEventMapper {
    
    @Mapping(target = "userEmail", ignore = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "action", source = "action")
    @Mapping(target = "entityType", source = "entityType")
    @Mapping(target = "entityId", source = "entityId")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "details", source = "details")
    AuditEventResponse toAuditEventResponse(AuditEvent auditEvent);

    @AfterMapping
    default void setUserEmail(@MappingTarget AuditEventResponse response, AuditEvent auditEvent) {
        if (auditEvent.getUser() != null) {
            response.setUserEmail(auditEvent.getUser().getEmail());
        }
    }

    List<AuditEventResponse> toAuditEventResponseList(List<AuditEvent> auditEvents);
}
