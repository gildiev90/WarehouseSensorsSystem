package com.warehouse.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class WarehousePropertiesValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidPortsLimitsAndTimeouts() {
        WarehouseProperties properties = new WarehouseProperties(
                "warehouse-001", 0, 65536, "http://localhost:8080", 0, 0, Duration.ZERO, Duration.ZERO);

        Set<String> invalidProperties = validator.validate(properties).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "temperatureUdpPort",
                "humidityUdpPort",
                "maxInFlightRequests",
                "maxPendingSensors",
                "httpResponseTimeout",
                "maxMeasurementAge"), invalidProperties);
    }
}
