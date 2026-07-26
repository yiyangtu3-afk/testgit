package com.campuslink.gateway.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GatewayFallbackControllerTest {

  private final GatewayFallbackController controller = new GatewayFallbackController();

  @Test
  void returnsAVisibleServiceUnavailableResponseForActivityCircuit() {
    var response = controller.unavailable("activity").block();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode().value()).isEqualTo(503);
    assertThat(response.getBody()).containsEntry("message", "活动服务暂时不可用，请稍后重试");
  }
}
