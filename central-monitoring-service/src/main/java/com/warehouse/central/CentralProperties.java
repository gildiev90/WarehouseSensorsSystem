package com.warehouse.central;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "central")
public record CentralProperties(
        int httpPort,
        double temperatureThreshold,
        double humidityThreshold,
        Duration maxMeasurementAge) {
}
