package com.warehouse.central;

import java.time.Instant;

public record Measurement(String warehouseId, String sensorId, MeasurementType type, Double value, Instant receivedAt) {
}
