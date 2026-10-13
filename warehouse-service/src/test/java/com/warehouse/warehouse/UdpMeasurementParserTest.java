package com.warehouse.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UdpMeasurementParserTest {
    private final UdpMeasurementParser parser = new UdpMeasurementParser();

    @Test
    void parsesTemperatureWithWhitespaceAndNegativeFraction() {
        Measurement measurement = parser.parse(" sensor_id = t1 ; value = -2.5 ",
                MeasurementType.TEMPERATURE, "warehouse-001", Instant.EPOCH);

        assertEquals("t1", measurement.sensorId());
        assertEquals(-2.5, measurement.value());
    }

    @Test
    void rejectsInvalidHumidityAndNan() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=h1;value=101",
                MeasurementType.HUMIDITY, "warehouse-001", Instant.EPOCH));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1;value=NaN",
                MeasurementType.TEMPERATURE, "warehouse-001", Instant.EPOCH));
    }

    @Test
    void rejectsDuplicateFields() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                "sensor_id=trusted;sensor_id=attacker;value=20",
                MeasurementType.TEMPERATURE, "warehouse-001", Instant.EPOCH));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                "sensor_id=t1;value=20;value=30",
                MeasurementType.TEMPERATURE, "warehouse-001", Instant.EPOCH));
    }

    @Test
    void rejectsMalformedIncompleteAndUnknownFields() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1;value",
                MeasurementType.TEMPERATURE, "warehouse-001", Instant.EPOCH));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=;value=20",
                MeasurementType.TEMPERATURE, "warehouse-001", Instant.EPOCH));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1;value=20;unit=celsius",
                MeasurementType.TEMPERATURE, "warehouse-001", Instant.EPOCH));
    }

    @Test
    void rejectsNonNumericAndInfiniteValues() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1;value=invalid",
                MeasurementType.TEMPERATURE, "warehouse-001", Instant.EPOCH));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1;value=Infinity",
                MeasurementType.TEMPERATURE, "warehouse-001", Instant.EPOCH));
    }
}
