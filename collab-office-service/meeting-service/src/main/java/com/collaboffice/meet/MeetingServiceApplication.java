package com.collaboffice.meet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.collaboffice")
public class MeetingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingServiceApplication.class, args);
    }
}
