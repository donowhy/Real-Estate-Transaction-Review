package org.micro.myservice.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EntityScan({"org.micro.myservice.inventory", "org.micro.myservice.logistics.persistence"})
@EnableJpaRepositories("org.micro.myservice.inventory")
@ComponentScan({"org.micro.myservice.inventory", "org.micro.myservice.logistics"})
@SpringBootApplication
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
