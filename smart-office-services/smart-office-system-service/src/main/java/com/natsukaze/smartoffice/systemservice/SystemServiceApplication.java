package com.natsukaze.smartoffice.systemservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.natsukaze.smartoffice.api")
@EnableDiscoveryClient
@SpringBootApplication
public class SystemServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemServiceApplication.class, args);
    }
}
