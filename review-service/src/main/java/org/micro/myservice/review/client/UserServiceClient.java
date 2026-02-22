package org.micro.myservice.review.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/health")
    String getUserServiceStatus();

    @GetMapping("/users/{userId}")
    UserResponse getUserById(@PathVariable("userId") Long userId);

    @Data
    class UserResponse {
        private Long id;
        private String name;
        private String email;
    }
}
