package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campuslink.entity.DemoEntities.ModerationItemEntity;
import com.campuslink.repository.AdminMetricsRepository;
import com.campuslink.repository.AuditRepository;
import com.campuslink.repository.FeedRepository;
import com.campuslink.repository.ModerationRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AdminServiceTest {

  @Test
  void rejectionRequiresCommentBeforeAnyModerationWrite() {
    FeedRepository feed = Mockito.mock(FeedRepository.class);
    ModerationRepository moderation = Mockito.mock(ModerationRepository.class);
    AuditRepository audits = Mockito.mock(AuditRepository.class);
    AdminMetricsRepository metrics = Mockito.mock(AdminMetricsRepository.class);
    AuditService auditService = new AuditService(audits);
    AdminService service = new AdminService(
        feed,
        moderation,
        audits,
        metrics,
        auditService,
        new ModerationAssistanceService(),
        fixedClock());
    when(moderation.findById("m-1")).thenReturn(Optional.of(pendingItem()));

    assertThatThrownBy(() -> service.resolveModeration("m-1", "reject", "教务管理员", "  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("拒绝内容时必须填写审核意见");

    verify(moderation, never()).completeReview(
        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyString());
    verify(audits, never()).add(Mockito.anyString(), Mockito.anyString());
  }

  private DemoClock fixedClock() {
    return new DemoClock() {
      @Override
      public LocalDateTime now() {
        return LocalDateTime.of(2026, 7, 20, 10, 30);
      }
    };
  }

  private ModerationItemEntity pendingItem() {
    return new ModerationItemEntity(
        "m-1", "post", 1L, null, "动态：审核内容", "林一", "审核内容", "pending",
        "校园动态发布审核", null, null, null, "2026-07-20 09:00", "09:00");
  }
}
