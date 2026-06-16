package com.natsukaze.smartoffice.orgservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.natsukaze.smartoffice.api")
@EnableDiscoveryClient
@SpringBootApplication
public class OrgServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrgServiceApplication.class, args);
    }
}
