package com.kidsbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KidsBookApplication {
    public static void main(String[] args) {
        SpringApplication.run(KidsBookApplication.class, args);
    }
}
