package org.micro.myservice.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EnableCaching
@EnableScheduling
@EntityScan({"org.micro.myservice.order", "org.micro.myservice.logistics.persistence"})
@EnableJpaRepositories("org.micro.myservice.order")
@ComponentScan({"org.micro.myservice.order", "org.micro.myservice.logistics"})
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
