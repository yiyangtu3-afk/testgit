package com.campuslink.controller;

import com.campuslink.dto.DemoDtos.AdminReportView;
import com.campuslink.dto.DemoDtos.AuditEventView;
import com.campuslink.dto.DemoDtos.DeleteAuditEventsRequest;
import com.campuslink.dto.DemoDtos.DeleteAuditEventsResponse;
import com.campuslink.dto.DemoDtos.DeleteModerationRequest;
import com.campuslink.dto.DemoDtos.DeleteModerationResponse;
import com.campuslink.dto.DemoDtos.ModerationItemView;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.AdminService;
import com.campuslink.service.AuditService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final AdminService adminService;
  private final AuditService auditService;
  private final AuthTokenService authTokenService;

  public AdminController(
      AdminService adminService,
      AuditService auditService,
      AuthTokenService authTokenService) {
    this.adminService = adminService;
    this.auditService = auditService;
    this.authTokenService = authTokenService;
  }

  @GetMapping("/metrics")
  public Map<String, String> metrics(@RequestHeader(value = "Authorization", required = false) String authorization) {
    authTokenService.requireAdmin(authorization);
    return adminService.metrics();
  }

  @GetMapping("/moderation")
  public List<ModerationItemView> moderationItems(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    authTokenService.requireAdmin(authorization);
    return adminService.moderationItems();
  }

  @PostMapping("/moderation/{itemId}/{decision}")
  public ModerationItemView resolveModeration(
      @PathVariable String itemId,
      @PathVariable String decision,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    UserEntity user = authTokenService.requireAdmin(authorization);
    return adminService.resolveModeration(itemId, decision, user.name());
  }

  @DeleteMapping("/moderation")
  public DeleteModerationResponse deleteModerationItems(
      @RequestBody DeleteModerationRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    UserEntity user = authTokenService.requireAdmin(authorization);
    return adminService.deleteModerationItems(request.itemIds(), user.name());
  }

  @GetMapping("/report")
  public AdminReportView adminReport(
      @RequestParam(required = false) String range,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    UserEntity user = authTokenService.requireAdmin(authorization);
    return adminService.adminReport(range, user.name());
  }

  @GetMapping("/audit-events")
  public List<AuditEventView> auditEvents(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    authTokenService.requireAdmin(authorization);
    return auditService.auditEvents();
  }

  @DeleteMapping("/audit-events")
  public DeleteAuditEventsResponse deleteAuditEvents(
      @RequestBody DeleteAuditEventsRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    authTokenService.requireAdmin(authorization);
    return auditService.deleteAuditEvents(request.eventIds());
  }
}
