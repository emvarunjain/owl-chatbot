package com.owl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OwlApplication {
    public static void main(String[] args) {
        SpringApplication.run(OwlApplication.class, args);
    }
}
