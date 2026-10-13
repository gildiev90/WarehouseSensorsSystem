package com.warehouse.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

class MeasurementDispatcherTest {
    private final CentralServerClient centralServerClient = mock(CentralServerClient.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    @Test
    void sendsFreshMeasurementAndPublishesSuccess() {
        when(centralServerClient.sendMeasurement(any())).thenReturn(Mono.empty());
        Measurement measurement = measurement("sensor-1", 20);

        dispatcher(1, 1).submit(measurement, 3344);

        verify(centralServerClient).sendMeasurement(measurement);
        ArgumentCaptor<CentralRequestCompletedEvent> event =
                ArgumentCaptor.forClass(CentralRequestCompletedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(3344, event.getValue().port());
        assertEquals(measurement.receivedAt(), event.getValue().receivedAt());
        assertEquals(RequestOutcome.SUCCESS, event.getValue().outcome());
    }

    @Test
    void publishesErrorAndReleasesSlotWhenRequestFails() {
        when(centralServerClient.sendMeasurement(any())).thenReturn(Mono.error(new IllegalStateException("down")));

        dispatcher(1, 1).submit(measurement("sensor-1", 20), 3344);

        ArgumentCaptor<CentralRequestCompletedEvent> event =
                ArgumentCaptor.forClass(CentralRequestCompletedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(RequestOutcome.ERROR, event.getValue().outcome());
    }

    @Test
    void dropsExpiredMeasurementBeforeSending() {
        MeasurementDispatcher dispatcher = dispatcher(1, 1);
        Measurement expired = new Measurement("warehouse-001", "sensor-1", MeasurementType.TEMPERATURE,
                20, Instant.now().minusSeconds(61));

        dispatcher.submit(expired, 3344);

        verifyNoInteractions(centralServerClient, eventPublisher);
    }

    @Test
    void sendsOnlyNewestQueuedMeasurementForSensorAfterSlotIsReleased() {
        Sinks.One<Void> firstRequest = Sinks.one();
        when(centralServerClient.sendMeasurement(any()))
                .thenReturn(firstRequest.asMono())
                .thenReturn(Mono.empty());
        MeasurementDispatcher dispatcher = dispatcher(1, 1);

        dispatcher.submit(measurement("sensor-1", 20), 3344);
        dispatcher.submit(measurement("sensor-2", 21), 3344);
        dispatcher.submit(measurement("sensor-2", 22), 3344);
        assertEquals(1, dispatcher.pendingCount());

        firstRequest.tryEmitEmpty();

        ArgumentCaptor<Measurement> requests = ArgumentCaptor.forClass(Measurement.class);
        verify(centralServerClient, times(2)).sendMeasurement(requests.capture());
        assertEquals(List.of(20.0, 22.0),
                requests.getAllValues().stream().map(Measurement::value).toList());
        assertEquals(0, dispatcher.pendingCount());
    }

    @Test
    void evictsOldestQueuedSensorWhenQueueIsFull() {
        Sinks.One<Void> firstRequest = Sinks.one();
        when(centralServerClient.sendMeasurement(any()))
                .thenReturn(firstRequest.asMono())
                .thenReturn(Mono.empty());
        MeasurementDispatcher dispatcher = dispatcher(1, 1);

        dispatcher.submit(measurement("sensor-1", 20), 3344);
        dispatcher.submit(measurement("sensor-2", 21), 3344);
        dispatcher.submit(measurement("sensor-3", 22), 3344);

        firstRequest.tryEmitEmpty();

        ArgumentCaptor<Measurement> requests = ArgumentCaptor.forClass(Measurement.class);
        verify(centralServerClient, times(2)).sendMeasurement(requests.capture());
        assertEquals(List.of("sensor-1", "sensor-3"),
                requests.getAllValues().stream().map(Measurement::sensorId).toList());
    }

    private MeasurementDispatcher dispatcher(int maxInFlightRequests, int maxPendingSensors) {
        WarehouseProperties properties = new WarehouseProperties(
                "warehouse-001", 3344, 3355, "http://localhost:8080",
                maxInFlightRequests, maxPendingSensors, Duration.ofSeconds(1), Duration.ofMinutes(1));
        return new MeasurementDispatcher(properties, centralServerClient, eventPublisher);
    }

    private Measurement measurement(String sensorId, double value) {
        return new Measurement("warehouse-001", sensorId, MeasurementType.TEMPERATURE, value, Instant.now());
    }
}
