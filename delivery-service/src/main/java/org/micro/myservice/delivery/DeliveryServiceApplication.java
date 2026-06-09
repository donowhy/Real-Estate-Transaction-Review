package org.micro.myservice.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EntityScan({"org.micro.myservice.delivery", "org.micro.myservice.logistics.persistence"})
@EnableJpaRepositories("org.micro.myservice.delivery")
@ComponentScan({"org.micro.myservice.delivery", "org.micro.myservice.logistics"})
@SpringBootApplication
public class DeliveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
