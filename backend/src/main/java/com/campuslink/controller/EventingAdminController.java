package com.campuslink.controller;

import com.campuslink.dto.EventingDtos.EventingOperationsView;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.eventing.EventingOperationsService;
import com.campuslink.service.AuthTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/eventing")
public class EventingAdminController {

  private final EventingOperationsService operations;
  private final AuthTokenService authTokens;

  public EventingAdminController(EventingOperationsService operations, AuthTokenService authTokens) {
    this.operations = operations;
    this.authTokens = authTokens;
  }

  @GetMapping("/operations")
  public EventingOperationsView operations(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    authTokens.requireAdmin(authorization);
    return operations.operations();
  }

  @PostMapping("/dead-letters/{source}/{id}/replay")
  public void replay(
      @PathVariable String source,
      @PathVariable String id,
      @RequestParam(defaultValue = "false") boolean confirm,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    if (!confirm) {
      throw new IllegalArgumentException("重放死信事件前必须明确确认");
    }
    UserEntity administrator = authTokens.requireAdmin(authorization);
    operations.replay(source, id, administrator.name());
  }
}
