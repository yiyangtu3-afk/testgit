package com.campuslink.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.activity.api.ActivityDtos.CreateActivityRequest;
import com.campuslink.activity.api.ActivityDtos.ReviewActivityRequest;
import com.campuslink.activity.cache.NoopActivityCatalogCache;
import com.campuslink.activity.domain.ActivityRecord;
import com.campuslink.activity.domain.UserDirectoryEntry;
import com.campuslink.activity.eventing.ActivityCatalogChangedEvent;
import com.campuslink.activity.eventing.ActivityReviewEventPublisher;
import com.campuslink.activity.mapper.ActivityMapper;
import com.campuslink.activity.mapper.OutboxEventMapper;
import com.campuslink.activity.mapper.UserDirectoryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

class ActivityApplicationServiceTest {

  @Test
  void createsPendingActivityAndRequiresOrganizerRole() {
    var store = new RecordingActivities();
    var outbox = new RecordingOutbox();
    var service = service(store, outbox);
    var teacher = new UserDirectoryEntry("teacher-1", "李老师", "教师");

    var result = service.create(teacher, request());

    assertThat(result.status()).isEqualTo("pending");
    assertThat(store.reviews).extracting(review -> review.decision).containsExactly("submitted");
    assertThatThrownBy(() -> service.create(new UserDirectoryEntry("student-1", "林一", "学生"), request()))
        .isInstanceOf(ResponseStatusException.class).hasMessageContaining("只有教师");
  }

  @Test
  void reviewsOnceAndPublishesReviewEventAfterTheTransactionalWork() {
    var store = new RecordingActivities();
    var outbox = new RecordingOutbox();
    var events = new ArrayList<Object>();
    var service = service(store, outbox, events::add);
    var created = service.create(new UserDirectoryEntry("teacher-1", "李老师", "教师"), request());

    var reviewed = service.review(new UserDirectoryEntry("admin-1", "管理员", "教务管理员"),
        created.id(), new ReviewActivityRequest("reject", "时间信息不完整"));

    assertThat(reviewed.status()).isEqualTo("draft");
    assertThat(reviewed.reviewReason()).isEqualTo("时间信息不完整");
    assertThat(outbox.events).singleElement().satisfies(event -> {
      assertThat(event.type).isEqualTo("activity.review.rejected.v1");
      assertThat(event.payload).contains("时间信息不完整");
    });
    assertThat(events).singleElement().isInstanceOf(ActivityCatalogChangedEvent.class)
        .extracting(event -> ((ActivityCatalogChangedEvent) event).activityId())
        .isEqualTo(created.id());
    assertThatThrownBy(() -> service.review(new UserDirectoryEntry("admin-1", "管理员", "教务管理员"),
        created.id(), new ReviewActivityRequest("approve", null))).isInstanceOf(IllegalArgumentException.class);
  }

  private ActivityApplicationService service(RecordingActivities store, RecordingOutbox outbox) {
    return service(store, outbox, event -> {});
  }

  private ActivityApplicationService service(
      RecordingActivities store, RecordingOutbox outbox, ApplicationEventPublisher events) {
    UserDirectoryMapper users = id -> Map.of(
        "teacher-1", new UserDirectoryEntry("teacher-1", "李老师", "教师"),
        "admin-1", new UserDirectoryEntry("admin-1", "管理员", "教务管理员")).get(id);
    return new ActivityApplicationService(store, users,
        new ActivityReviewEventPublisher(outbox, new ObjectMapper().findAndRegisterModules()),
        new NoopActivityCatalogCache(), events);
  }

  private CreateActivityRequest request() {
    return new CreateActivityRequest("校园编程赛", "面向全校学生的竞赛。", "竞赛", "创新中心",
        LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0), 30);
  }

  private static final class RecordingActivities implements ActivityMapper {
    private final Map<String, ActivityRecord> activities = new LinkedHashMap<>();
    private final List<Review> reviews = new ArrayList<>();
    @Override public void insert(String id, String organizerId, String title, String description, String category,
        String location, LocalDateTime startsAt, LocalDateTime endsAt, int capacity) {
      activities.put(id, new ActivityRecord(id, title, description, category, location, startsAt, endsAt,
          capacity, organizerId, "pending", "pending", null, null, null, LocalDateTime.now()));
    }
    @Override public void insertReview(String id, String activityId, String actorId, String decision, String reason) { reviews.add(new Review(decision, reason)); }
    @Override public ActivityRecord find(String id) { return activities.get(id); }
    @Override public ActivityRecord findForUpdate(String id) { return activities.get(id); }
    @Override public List<ActivityRecord> published(String category, LocalDateTime from, LocalDateTime before) { return List.of(); }
    @Override public List<ActivityRecord> managed(String organizerId) { return List.of(); }
    @Override public List<ActivityRecord> pending() { return List.of(); }
    @Override public int review(String id, String status, String decision, String reason, String reviewerId) {
      ActivityRecord activity = activities.get(id);
      if (activity == null || !"pending".equals(activity.status())) return 0;
      activities.put(id, new ActivityRecord(activity.id(), activity.title(), activity.description(),
          activity.category(), activity.location(), activity.startsAt(), activity.endsAt(), activity.capacity(),
          activity.organizerId(), status, decision, reason, reviewerId, LocalDateTime.now(), activity.createdAt()));
      return 1;
    }
    @Override public int updateRegistrationStatus(String id, String status) { return 1; }
    private record Review(String decision, String reason) {}
  }

  private static final class RecordingOutbox implements OutboxEventMapper {
    private final List<Event> events = new ArrayList<>();
    @Override public void insert(String id, String aggregateType, String aggregateId, String eventType,
        String payload) {
      events.add(new Event(eventType, payload));
    }
    private record Event(String type, String payload) {}
  }
}
