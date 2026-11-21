package org.example.lateticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class LateTicketApplication {

    public static void main(String[] args) {
        // All LocalDateTime values (holds, payment deadlines, createdAt) are UTC regardless of the host timezone,
        // so `spring-boot:run` locally behaves the same as the Docker image.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(LateTicketApplication.class, args);
    }

}
