package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.AuditEventEntity;
import com.campuslink.mapper.AuditMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAuditRepository implements AuditRepository {

  private final AuditMapper auditMapper;
  private final AtomicLong auditIds = new AtomicLong(System.currentTimeMillis() * 1000);

  public MyBatisAuditRepository(AuditMapper auditMapper) {
    this.auditMapper = auditMapper;
  }

  @Override
  public void add(String module, String event) {
    auditMapper.insert("a-" + auditIds.incrementAndGet(), module, event);
  }

  @Override
  public List<AuditEventEntity> findRecent(int limit) {
    return auditMapper.findRecent(limit);
  }

  @Override
  public int count() {
    return auditMapper.count();
  }

  @Override
  public int deleteByIds(List<String> eventIds) {
    if (eventIds == null || eventIds.isEmpty()) {
      return 0;
    }
    return auditMapper.deleteByIds(eventIds);
  }
}
