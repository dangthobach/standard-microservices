package com.enterprise.process;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@SpringBootApplication
@org.springframework.cloud.client.discovery.EnableDiscoveryClient
public class ProcessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessServiceApplication.class, args);
    }

}

