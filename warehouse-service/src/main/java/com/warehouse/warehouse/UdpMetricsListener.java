package com.warehouse.warehouse;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UdpMetricsListener {
    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<Integer, Counter> receivedDatagramCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<DurationMetricKey, Timer> endToEndTimers = new ConcurrentHashMap<>();

    public UdpMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void recordReceivedDatagram(UdpDatagramReceivedEvent event) {
        receivedDatagramCounters.computeIfAbsent(event.port(), this::registerReceivedDatagramCounter)
                .increment();
    }

    @EventListener
    public void recordEndToEndDuration(CentralRequestCompletedEvent event) {
        Duration duration = Duration.between(event.receivedAt(), event.completedAt());
        DurationMetricKey key = new DurationMetricKey(event.port(), event.outcome());
        endToEndTimers.computeIfAbsent(key, this::registerEndToEndTimer)
                .record(duration.isNegative() ? Duration.ZERO : duration);
    }

    private Counter registerReceivedDatagramCounter(int port) {
        return Counter.builder("warehouse.udp.requests")
                .description("Number of UDP datagrams received")
                .tag("port", Integer.toString(port))
                .register(meterRegistry);
    }

    private Timer registerEndToEndTimer(DurationMetricKey key) {
        return Timer.builder("warehouse.udp.request.duration")
                .description("Time from receiving a UDP datagram to completing the central service request")
                .tag("port", Integer.toString(key.port()))
                .tag("outcome", key.outcome().metricValue())
                .register(meterRegistry);
    }

    private record DurationMetricKey(int port, RequestOutcome outcome) {
    }
}
