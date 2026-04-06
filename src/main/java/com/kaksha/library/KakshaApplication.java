package com.kaksha.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KakshaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KakshaApplication.class, args);
    }
}
