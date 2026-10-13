package com.warehouse.warehouse;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class UdpMeasurementParser {
    public Measurement parse(String payload, MeasurementType type, String warehouseId, Instant receivedAt) {
        Map<String, String> fields = new HashMap<>();
        for (String part : payload.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2 || pair[0].trim().isEmpty()) {
                throw new IllegalArgumentException("Malformed key/value field");
            }
            if (fields.put(pair[0].trim(), pair[1].trim()) != null) {
                throw new IllegalArgumentException("Duplicate field");
            }
        }
        String sensorId = fields.get("sensor_id");
        String valueText = fields.get("value");

        if (sensorId == null || sensorId.isBlank() || valueText == null || fields.size() != 2) {
            throw new IllegalArgumentException("Expected non-empty sensor_id and value");
        }

        double value = validateSensorValue(type, valueText);

        return new Measurement(warehouseId, sensorId, type, value, receivedAt);
    }

    private double validateSensorValue(MeasurementType type, String valueText) {
        final double value;
        try {
            value = Double.parseDouble(valueText);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Measurement value is not numeric", exception);
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Measurement value must be finite");
        }
        if (type == MeasurementType.HUMIDITY && (value < 0 || value > 100)) {
            throw new IllegalArgumentException("Humidity must be between 0 and 100");
        }
        return value;
    }
}
