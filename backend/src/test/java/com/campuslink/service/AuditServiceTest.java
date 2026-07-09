package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.dto.DemoDtos.DeleteAuditEventsResponse;
import org.junit.jupiter.api.Test;

class AuditServiceTest {

  private final TestAuditRepository auditRepository = new TestAuditRepository();
  private final AuditService auditService = new AuditService(auditRepository);

  @Test
  void auditEventsExposeStableIds() {
    auditService.addAudit("后台", "教务管理员查看审计记录");

    assertThat(auditService.auditEvents())
        .extracting(event -> event.id())
        .containsExactly("a-1");
  }

  @Test
  void deleteAuditEventsDeduplicatesIds() {
    auditService.addAudit("后台", "第一条记录");
    auditService.addAudit("后台", "第二条记录");

    DeleteAuditEventsResponse response = auditService.deleteAuditEvents(
        java.util.List.of("a-1", "a-1", "", "a-2"));

    assertThat(response.deleted()).isEqualTo(2);
    assertThat(auditService.auditEvents()).isEmpty();
  }
}
