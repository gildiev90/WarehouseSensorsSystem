package com.warehouse.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class UdpMetricsListenerTest {
    @Test
    void recordsReceivedDatagramsSeparatelyByPort() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        UdpMetricsListener listener = new UdpMetricsListener(meterRegistry);

        listener.recordReceivedDatagram(new UdpDatagramReceivedEvent(3344));
        listener.recordReceivedDatagram(new UdpDatagramReceivedEvent(3344));
        listener.recordReceivedDatagram(new UdpDatagramReceivedEvent(3355));

        assertEquals(2.0, meterRegistry.get("warehouse.udp.requests").tag("port", "3344").counter().count());
        assertEquals(1.0, meterRegistry.get("warehouse.udp.requests").tag("port", "3355").counter().count());
    }

    @Test
    void recordsEndToEndDurationByPortAndOutcome() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        UdpMetricsListener listener = new UdpMetricsListener(meterRegistry);
        Instant receivedAt = Instant.parse("2026-10-11T16:00:00Z");

        listener.recordEndToEndDuration(new CentralRequestCompletedEvent(
                3344, receivedAt, receivedAt.plusMillis(250), RequestOutcome.SUCCESS));
        listener.recordEndToEndDuration(new CentralRequestCompletedEvent(
                3355, receivedAt, receivedAt.plusMillis(500), RequestOutcome.ERROR));

        assertEquals(0.25, meterRegistry.get("warehouse.udp.request.duration")
                .tags("port", "3344", "outcome", "success").timer().totalTime(TimeUnit.SECONDS));
        assertEquals(0.5, meterRegistry.get("warehouse.udp.request.duration")
                .tags("port", "3355", "outcome", "error").timer().totalTime(TimeUnit.SECONDS));
    }
}
