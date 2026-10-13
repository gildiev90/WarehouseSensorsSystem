package com.warehouse.warehouse;

import io.netty.channel.socket.DatagramPacket;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

@Component
public class UdpReceiver implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(UdpReceiver.class);
    private final WarehouseProperties properties;
    private final UdpMeasurementParser parser;
    private final MeasurementDispatcher dispatcher;
    private final ApplicationEventPublisher eventPublisher;
    private volatile boolean running;
    private Connection temperatureServer;
    private Connection humidityServer;

    public UdpReceiver(WarehouseProperties properties, UdpMeasurementParser parser, MeasurementDispatcher dispatcher,
                       ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.parser = parser;
        this.dispatcher = dispatcher;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void start() {
        temperatureServer = bind(properties.temperatureUdpPort(), MeasurementType.TEMPERATURE);
        humidityServer = bind(properties.humidityUdpPort(), MeasurementType.HUMIDITY);
        running = true;
    }

    private Connection bind(int port, MeasurementType type) {
        Connection server = UdpServer.create()
                .host("0.0.0.0")
                .port(port)
                .handle((in, out) -> in.receiveObject()
                        .ofType(DatagramPacket.class)
                        .doOnNext(packet -> {
                            eventPublisher.publishEvent(new UdpDatagramReceivedEvent(port));
                            accept(packet.content().toString(io.netty.util.CharsetUtil.UTF_8), type, port);
                        })
                        .then())
                .bindNow();
        log.info("UDP listener started type={} port={}", type, port);
        return server;
    }

    private void accept(String payload, MeasurementType type, int port) {
        try {
            log.info("Received UDP packet type={} payload={}", type, payload);
            dispatcher.submit(parser.parse(payload, type, properties.warehouseId(), Clock.systemUTC().instant()), port);
        } catch (IllegalArgumentException exception) {
            log.debug("Dropped invalid UDP packet type={}: {}", type, exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("Unexpected UDP packet processing failure type={}", type, exception);
        }
    }

    @Override
    public void stop() {
        if (temperatureServer != null) {
            temperatureServer.disposeNow();
        }
        if (humidityServer != null) {
            humidityServer.disposeNow();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
