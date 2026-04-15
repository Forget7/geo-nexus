package com.geonexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GeoNexusApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GeoNexusApplication.class, args);
    }
}
