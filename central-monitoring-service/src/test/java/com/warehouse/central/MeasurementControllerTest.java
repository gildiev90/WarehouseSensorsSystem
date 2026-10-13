package com.warehouse.central;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(MeasurementController.class)
@Import(MeasurementProcessor.class)
class MeasurementControllerTest {
    @Autowired
    private WebTestClient client;

    @Test
    void acceptsValidMeasurement() {
        client.post().uri("/api/v1/measurements")
                .bodyValue(new Measurement("warehouse-001", "t1", MeasurementType.TEMPERATURE, 36.0, Instant.now()))
                .exchange().expectStatus().isNoContent();
    }

    @Test
    void rejectsInvalidHumidity() {
        client.post().uri("/api/v1/measurements")
                .bodyValue(new Measurement("warehouse-001", "h1", MeasurementType.HUMIDITY, 101.0, Instant.now()))
                .exchange().expectStatus().isBadRequest();
    }
}
