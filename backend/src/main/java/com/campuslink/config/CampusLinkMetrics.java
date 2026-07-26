package com.campuslink.config;

import com.campuslink.repository.AdminMetricsRepository;
import com.campuslink.repository.FeedRepository;
import com.campuslink.repository.ModerationRepository;
import com.campuslink.repository.OutboxEventRepository;
import com.campuslink.service.DemoClock;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CampusLinkMetrics {

  public CampusLinkMetrics(
      MeterRegistry meterRegistry,
      AdminMetricsRepository adminMetricsRepository,
      FeedRepository feedRepository,
      ModerationRepository moderationRepository,
      OutboxEventRepository outboxEvents,
      DemoClock clock) {
    Gauge.builder(
            "campuslink.users.total",
            adminMetricsRepository,
            AdminMetricsRepository::countUsers)
        .description("Current CampusLink user count")
        .register(meterRegistry);
    Gauge.builder(
            "campuslink.messages.today",
            adminMetricsRepository,
            repository -> repository.countMessagesSince(clock.now().toLocalDate().atStartOfDay()))
        .description("Messages created since the current day started")
        .register(meterRegistry);
    Gauge.builder("campuslink.posts.total", feedRepository, FeedRepository::countPosts)
        .description("Current CampusLink post count")
        .register(meterRegistry);
    Gauge.builder(
            "campuslink.moderation.pending",
            moderationRepository,
            ModerationRepository::countPending)
        .description("Current pending moderation count")
        .register(meterRegistry);
    registerOutboxGauge(meterRegistry, outboxEvents, "pending");
    registerOutboxGauge(meterRegistry, outboxEvents, "retry");
    registerOutboxGauge(meterRegistry, outboxEvents, "dead_letter");
  }

  private void registerOutboxGauge(
      MeterRegistry meterRegistry, OutboxEventRepository events, String status) {
    Gauge.builder("campuslink.outbox.events", events, repository -> repository.countByStatus(status))
        .tag("status", status)
        .description("CampusLink Outbox events grouped by delivery status")
        .register(meterRegistry);
  }
}
