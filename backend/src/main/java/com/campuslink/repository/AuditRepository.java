package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.AuditEventEntity;
import java.util.List;

public interface AuditRepository {

  void add(String module, String event);

  List<AuditEventEntity> findRecent(int limit);

  int count();

  int deleteByIds(List<String> eventIds);
}
