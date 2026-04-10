package com.haru_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HaruBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HaruBackendApplication.class, args);
    }

}