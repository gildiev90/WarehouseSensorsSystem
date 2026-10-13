package com.warehouse.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class CentralServerClientTest {
    @Test
    void postsMeasurementsToCentralServiceEndpoint() {
        AtomicReference<ClientRequest> request = new AtomicReference<>();
        ExchangeFunction exchange = clientRequest -> {
            request.set(clientRequest);
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };
        WarehouseProperties properties = new WarehouseProperties(
                "warehouse-001", 3344, 3355, "http://central.example",
                1, 1, Duration.ofSeconds(1), Duration.ofSeconds(5));
        CentralServerClient client = new CentralServerClient(
                WebClient.builder().exchangeFunction(exchange).build(), properties);

        client.sendMeasurement(new Measurement("warehouse-001", "sensor-1", MeasurementType.TEMPERATURE,
                20, Instant.EPOCH)).block();

        assertEquals(HttpMethod.POST, request.get().method());
        assertEquals("/api/v1/measurements", request.get().url().getPath());
    }
}
