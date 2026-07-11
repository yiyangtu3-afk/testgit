package com.campuslink.service;

import com.campuslink.dto.DemoDtos.AdminReportView;
import com.campuslink.dto.DemoDtos.AuditEventView;
import com.campuslink.dto.DemoDtos.DeleteModerationResponse;
import com.campuslink.dto.DemoDtos.ModerationItemView;
import com.campuslink.dto.DemoDtos.ReportRangeView;
import com.campuslink.entity.DemoEntities.ModerationItemEntity;
import com.campuslink.repository.AuditRepository;
import com.campuslink.repository.FeedRepository;
import com.campuslink.repository.ModerationRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

  private final FeedRepository feedRepository;
  private final ModerationRepository moderationRepository;
  private final AuditRepository auditRepository;
  private final AuditService auditService;
  private final DemoClock clock;

  public AdminService(
      FeedRepository feedRepository,
      ModerationRepository moderationRepository,
      AuditRepository auditRepository,
      AuditService auditService,
      DemoClock clock) {
    this.feedRepository = feedRepository;
    this.moderationRepository = moderationRepository;
    this.auditRepository = auditRepository;
    this.auditService = auditService;
    this.clock = clock;
  }

  public Map<String, String> metrics() {
    Map<String, String> metrics = new LinkedHashMap<>();
    metrics.put("注册用户", "128");
    metrics.put("今日消息", "436");
    metrics.put("动态总数", String.valueOf(feedRepository.findVisiblePosts().size()));
    metrics.put("待审内容", String.valueOf(moderationRepository.countPending()));
    return metrics;
  }

  public List<ModerationItemView> moderationItems() {
    return moderationRepository.findPending().stream()
        .map(DemoMapper::toModerationItemView)
        .toList();
  }

  @Transactional
  public ModerationItemView resolveModeration(String itemId, String decision, String operatorName) {
    String status = switch (decision) {
      case "approve" -> "approved";
      case "reject" -> "rejected";
      default -> throw new IllegalArgumentException("审核动作不支持");
    };

    ModerationItemEntity item = moderationRepository.findById(itemId)
        .orElseThrow(() -> new IllegalArgumentException("审核记录不存在"));
    moderationRepository.updateStatus(itemId, status);
    ModerationItemEntity updated = new ModerationItemEntity(
        item.id(),
        item.type(),
        item.targetId(),
        item.postId(),
        item.title(),
        item.author(),
        item.body(),
        status,
        item.reason(),
        item.submittedAt(),
        item.time());
    applyModeration(updated);
    auditService.addAudit("审核", operatorName + ("approved".equals(status) ? "通过" : "拒绝")
        + item.author() + "的" + ("post".equals(item.type()) ? "动态" : "评论"));
    return DemoMapper.toModerationItemView(updated);
  }

  public DeleteModerationResponse deleteModerationItems(List<String> itemIds, String operatorName) {
    List<String> cleanIds = itemIds == null
        ? List.of()
        : itemIds.stream()
            .filter(itemId -> itemId != null && !itemId.isBlank())
            .distinct()
            .toList();
    int deleted = moderationRepository.deleteByIds(cleanIds);
    if (deleted > 0) {
      auditService.addAudit("审核", operatorName + "删除" + deleted + "条审核记录");
    }
    return new DeleteModerationResponse(deleted);
  }

  public AdminReportView adminReport(String range, String operatorName) {
    String selectedRange = normalizeReportRange(range);
    ReportRangeView reportRange = new ReportRangeView(selectedRange, reportRangeLabel(selectedRange));
    int auditLimit = switch (selectedRange) {
      case "all" -> auditRepository.count() + 1;
      case "week" -> 8;
      default -> 4;
    };
    auditService.addAudit("后台", operatorName + "导出" + reportRange.label() + "后台报表");
    List<AuditEventView> auditEvents = auditRepository.findRecent(auditLimit).stream()
        .map(DemoMapper::toAuditEventView)
        .toList();
    return new AdminReportView(
        clock.nowTime(),
        "campuslink-admin-report-" + selectedRange + ".csv",
        reportRange,
        metrics(),
        moderationRepository.findPending().stream()
            .map(DemoMapper::toModerationItemView)
            .toList(),
        auditEvents);
  }

  private void applyModeration(ModerationItemEntity item) {
    if ("post".equals(item.type())) {
      feedRepository.updatePostModeration(item.targetId(), item.status());
      return;
    }

    feedRepository.updateCommentModeration(item.postId(), item.targetId(), item.status());
  }

  private String normalizeReportRange(String range) {
    if (range == null || range.isBlank()) {
      return "today";
    }
    return switch (range) {
      case "today", "week", "all" -> range;
      default -> "today";
    };
  }

  private String reportRangeLabel(String range) {
    return switch (range) {
      case "week" -> "本周";
      case "all" -> "全部";
      default -> "今日";
    };
  }
}
