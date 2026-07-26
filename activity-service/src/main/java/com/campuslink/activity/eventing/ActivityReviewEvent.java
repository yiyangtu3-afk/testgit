package com.campuslink.activity.eventing;

import com.campuslink.activity.domain.ActivityRecord;
import java.time.LocalDateTime;

public record ActivityReviewEvent(ActivityRecord activity, String reviewerId, LocalDateTime occurredAt) {}
