package com.campuslink.repository;

import com.campuslink.mapper.EventProcessingReceiptMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisEventProcessingReceiptRepository implements EventProcessingReceiptRepository {

  private final EventProcessingReceiptMapper mapper;

  public MyBatisEventProcessingReceiptRepository(EventProcessingReceiptMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean recordIfFirst(String consumerName, String eventId) {
    return mapper.insertIfAbsent(consumerName, eventId) == 1;
  }
}
