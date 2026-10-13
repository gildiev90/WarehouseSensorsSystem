package com.warehouse.central;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CentralProperties.class)
public class CentralMonitoringApplication {
    public static void main(String[] args) {
        SpringApplication.run(CentralMonitoringApplication.class, args);
    }
}
