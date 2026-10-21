package com.syncflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SyncFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncFlowApplication.class, args);
    }
}
