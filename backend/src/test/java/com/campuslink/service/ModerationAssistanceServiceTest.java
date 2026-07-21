package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.dto.DemoDtos.ModerationAssistanceView;
import com.campuslink.entity.DemoEntities.ModerationItemEntity;
import org.junit.jupiter.api.Test;

class ModerationAssistanceServiceTest {

  private final ModerationAssistanceService service = new ModerationAssistanceService();

  @Test
  void flagsHighRiskTermsWithoutResolvingTheItem() {
    ModerationAssistanceView result = service.analyze(item("提供代写服务，联系 13800000000"));

    assertThat(result.suggestedDecision()).isEqualTo("reject");
    assertThat(result.riskLevel()).isEqualTo("high");
    assertThat(result.signals()).contains("包含疑似手机号", "命中高风险词：代写");
    assertThat(result.provider()).isEqualTo("local-policy-v1");
  }

  @Test
  void keepsLowRiskContentAsARecommendationForHumanDecision() {
    ModerationAssistanceView result = service.analyze(item("本周社团招新分享会在图书馆举行。"));

    assertThat(result.suggestedDecision()).isEqualTo("approve");
    assertThat(result.riskLevel()).isEqualTo("low");
    assertThat(result.suggestedComment()).contains("管理员");
  }

  private ModerationItemEntity item(String body) {
    return new ModerationItemEntity(
        "m-1", "post", 1L, null, "动态：审核内容", "林一", body, "pending",
        "校园动态发布审核", null, null, null, "2026-07-20 09:00", "09:00");
  }
}
