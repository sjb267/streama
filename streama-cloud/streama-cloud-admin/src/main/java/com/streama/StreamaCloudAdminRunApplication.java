package com.streama;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.streama"})
@MapperScan(basePackages = {"com.streama.mappers"})
@EnableFeignClients
public class StreamaCloudAdminRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamaCloudAdminRunApplication.class, args);
    }
}
