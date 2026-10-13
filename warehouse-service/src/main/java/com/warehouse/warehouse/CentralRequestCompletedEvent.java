package com.warehouse.warehouse;

import java.time.Instant;

public record CentralRequestCompletedEvent(
        int port,
        Instant receivedAt,
        Instant completedAt,
        RequestOutcome outcome) {
}
