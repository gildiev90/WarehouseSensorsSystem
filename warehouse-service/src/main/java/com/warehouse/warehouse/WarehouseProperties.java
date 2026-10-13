package com.warehouse.warehouse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "warehouse")
public record WarehouseProperties(
        String warehouseId,

        @Min(1)
        @Max(65535)
        int temperatureUdpPort,

        @Min(1)
        @Max(65535)
        int humidityUdpPort,

        String centralServiceBaseUrl,

        @Positive
        int maxInFlightRequests,

        @Positive
        int maxPendingSensors,

        @DurationMin(seconds = 0, inclusive = false)
        Duration httpResponseTimeout,

        @DurationMin(seconds = 0, inclusive = false)
        Duration maxMeasurementAge) {
}
