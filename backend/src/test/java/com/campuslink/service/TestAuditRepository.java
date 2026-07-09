package com.campuslink.service;

import com.campuslink.entity.DemoEntities.AuditEventEntity;
import com.campuslink.repository.AuditRepository;
import java.util.ArrayList;
import java.util.List;

final class TestAuditRepository implements AuditRepository {

  private final List<AuditEventEntity> events = new ArrayList<>();

  @Override
  public void add(String module, String event) {
    events.add(0, new AuditEventEntity("a-" + (events.size() + 1), "09:30", module, event));
  }

  @Override
  public List<AuditEventEntity> findRecent(int limit) {
    return events.stream().limit(limit).toList();
  }

  @Override
  public int count() {
    return events.size();
  }

  @Override
  public int deleteByIds(List<String> eventIds) {
    int before = events.size();
    events.removeIf(event -> eventIds.contains(event.id()));
    return before - events.size();
  }
}
