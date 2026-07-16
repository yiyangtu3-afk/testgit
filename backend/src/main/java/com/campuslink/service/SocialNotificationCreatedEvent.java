package com.campuslink.service;

import com.campuslink.dto.SocialNotificationDtos.NotificationView;

public record SocialNotificationCreatedEvent(
    String recipientId, NotificationView notification) {
}
