package org.micro.myservice.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome to the User Service!";
    }

    @GetMapping("/health")
    public String status() {
        return "User Service is up and running!";
    }

    @GetMapping("/users/{userId}")
    public UserResponse getUserById(@PathVariable Long userId) {
        // 실제로는 DB에서 조회하겠지만, 지금은 테스트를 위해 Mock 데이터를 반환합니다.
        return new UserResponse(userId, "User-" + userId, "test@example.com");
    }

    // 간단한 내부 DTO (실제로는 별도 파일로 분리하는 것이 좋습니다.)
    public record UserResponse(Long id, String name, String email) {}
}
