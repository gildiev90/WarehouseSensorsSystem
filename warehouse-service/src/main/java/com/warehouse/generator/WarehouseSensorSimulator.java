package com.warehouse.generator;

import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class WarehouseSensorSimulator {

    private static final String HOST = "127.0.0.1";

    private static final int TEMP_PORT = 3344;
    private static final int TEMP_COUNT = 600;
    private static final int TEMP_MIN = 20;
    private static final int TEMP_MAX = 40;

    private static final int HUMIDITY_PORT = 3355;
    private static final int HUMIDITY_COUNT = 800;
    private static final int HUMIDITY_MIN = 30;
    private static final int HUMIDITY_MAX = 80;

    private static final long SENSOR_INTERVAL_MS = 5000;

    private static final int THREAD_POOL_SIZE = 8;

    private static final AtomicLong sent = new AtomicLong();
    private static final AtomicLong errors = new AtomicLong();

    public static void main(String[] args) throws Exception {

        InetAddress address = InetAddress.getByName(HOST);

        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);

        DatagramSocket socket = channel.socket();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);

        startSensors(
                scheduler, socket, address,
                TEMP_PORT, "t", TEMP_COUNT,
                TEMP_MIN, TEMP_MAX
        );

        startSensors(
                scheduler, socket, address,
                HUMIDITY_PORT, "h", HUMIDITY_COUNT,
                HUMIDITY_MIN, HUMIDITY_MAX
        );

        AtomicLong previousSent = new AtomicLong();

        scheduler.scheduleAtFixedRate(() -> {
            long total = sent.get();
            long delta = total - previousSent.getAndSet(total);

            System.out.printf(
                    "Sent: %d (%.1f packets/s), total: %d, errors: %d%n",
                    delta, delta / 5.0, total, errors.get()
            );
        }, 5, 5, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdownNow();
            try {
                channel.close();
            } catch (Exception ignored) {
            }
            System.out.println("Simulator stopped");
        }));

        System.out.println("Simulator started");
    }

    private static void startSensors(
            ScheduledExecutorService scheduler,
            DatagramSocket socket,
            InetAddress address,
            int port,
            String prefix,
            int count,
            int minValue,
            int maxValue
    ) {
        for (int i = 1; i <= count; i++) {

            final String sensorId = prefix + i;

            // Independent initial delay for each sensor
            long initialDelay = ThreadLocalRandom.current()
                    .nextLong(SENSOR_INTERVAL_MS);

            scheduler.scheduleAtFixedRate(
                    () -> sendMeasurement(
                            socket, address, port,
                            sensorId, minValue, maxValue
                    ),
                    initialDelay,
                    SENSOR_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
            );
        }

        System.out.printf(
                "Port %d: %d independent sensors%n",
                port, count
        );
    }

    private static void sendMeasurement(
            DatagramSocket socket,
            InetAddress address,
            int port,
            String sensorId,
            int minValue,
            int maxValue
    ) {
        int value = ThreadLocalRandom.current()
                .nextInt(minValue, maxValue + 1);

        String message = "sensor_id=" + sensorId +
                "; value=" + value;

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        DatagramPacket packet = new DatagramPacket(
                bytes, bytes.length, address, port
        );

        try {
            socket.send(packet);
            sent.incrementAndGet();
        } catch (Exception e) {
            errors.incrementAndGet();
            System.err.println("UDP send failed: " + e.getMessage());
        }
    }
}

