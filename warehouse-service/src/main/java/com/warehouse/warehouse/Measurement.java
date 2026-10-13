package com.warehouse.warehouse;

import java.time.Instant;

public record Measurement(
        String warehouseId,
        String sensorId,
        MeasurementType type,
        double value,
        Instant receivedAt) {
}
