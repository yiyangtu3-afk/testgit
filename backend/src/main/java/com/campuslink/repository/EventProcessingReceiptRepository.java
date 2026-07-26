package com.campuslink.repository;

public interface EventProcessingReceiptRepository {

  boolean recordIfFirst(String consumerName, String eventId);
}
