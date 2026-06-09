package org.micro.myservice.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EntityScan({"org.micro.myservice.notification", "org.micro.myservice.logistics.persistence"})
@EnableJpaRepositories("org.micro.myservice.notification")
@ComponentScan({"org.micro.myservice.notification", "org.micro.myservice.logistics"})
@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
