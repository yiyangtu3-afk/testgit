package com.campuslink.service;

import com.campuslink.dto.ActivityNotificationDtos.NotificationView;

public record ActivityNotificationCreatedEvent(
    String recipientId, NotificationView notification) {
}
