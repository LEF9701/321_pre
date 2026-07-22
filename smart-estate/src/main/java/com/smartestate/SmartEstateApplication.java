package com.smartestate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartEstateApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartEstateApplication.class, args);
    }
}
