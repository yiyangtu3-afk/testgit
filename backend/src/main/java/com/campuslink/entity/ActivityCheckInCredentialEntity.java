package com.campuslink.entity;

import java.time.LocalDateTime;

public record ActivityCheckInCredentialEntity(
    String id,
    String registrationId,
    String tokenHash,
    LocalDateTime issuedAt) {
}
