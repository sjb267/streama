package com.streama;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.streama"})
public class StreamaCloudGatewayRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamaCloudGatewayRunApplication.class, args);
    }
}
