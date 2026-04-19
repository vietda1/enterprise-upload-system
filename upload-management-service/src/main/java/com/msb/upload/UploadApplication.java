package com.msb.upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class UploadApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UploadApplication.class, args);
    }
}