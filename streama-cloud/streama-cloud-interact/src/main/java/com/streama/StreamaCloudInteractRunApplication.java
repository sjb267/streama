package com.streama;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.streama"})
@MapperScan(basePackages = {"com.streama.mappers"})
@EnableFeignClients
@EnableScheduling
public class StreamaCloudInteractRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamaCloudInteractRunApplication.class, args);
    }
}
