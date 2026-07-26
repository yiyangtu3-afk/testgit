package com.campuslink.eventing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campuslink.eventing")
public record EventingProperties(
    String activityTopic,
    String activityDeadLetterTopic,
    int outboxRetryDelaySeconds,
    int outboxMaxAttempts,
    long consumerRetryDelayMs,
    int consumerMaxAttempts) {
}
