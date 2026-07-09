package com.campuslink.service;

import com.campuslink.dto.DemoDtos.AuditEventView;
import com.campuslink.dto.DemoDtos.DeleteAuditEventsResponse;
import com.campuslink.repository.AuditRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

  private final AuditRepository auditRepository;

  public AuditService(AuditRepository auditRepository) {
    this.auditRepository = auditRepository;
  }

  public void addAudit(String module, String event) {
    auditRepository.add(module, event);
  }

  public List<AuditEventView> auditEvents() {
    return auditRepository.findRecent(Math.max(1, auditRepository.count())).stream()
        .map(DemoMapper::toAuditEventView)
        .toList();
  }

  public DeleteAuditEventsResponse deleteAuditEvents(List<String> eventIds) {
    List<String> cleanIds = eventIds == null
        ? List.of()
        : eventIds.stream()
            .filter(eventId -> eventId != null && !eventId.isBlank())
            .distinct()
            .toList();
    return new DeleteAuditEventsResponse(auditRepository.deleteByIds(cleanIds));
  }
}
