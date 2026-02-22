package org.micro.myservice.review.controller;

import lombok.RequiredArgsConstructor;
import org.micro.myservice.review.client.UserServiceClient;
import org.micro.myservice.review.dto.ReviewRequest;
import org.micro.myservice.review.dto.ReviewResponse;
import org.micro.myservice.review.entity.Review;
import org.micro.myservice.review.repository.ReviewRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final UserServiceClient userServiceClient;
    private final ReviewRepository reviewRepository;

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome to the Review Service!";
    }

    @GetMapping("/health")
    public String status() {
        return "Review Service is up and running!";
    }

    @GetMapping("/check-user-service")
    public String checkUserService() {
        return "Review Service says: " + userServiceClient.getUserServiceStatus();
    }

    @PostMapping("/reviews")
    public Review createReview(@RequestBody ReviewRequest request) {
        Review review = Review.builder()
                .propertyId(request.getPropertyId())
                .userId(request.getUserId())
                .rating(request.getRating())
                .content(request.getContent())
                .build();
        return reviewRepository.save(review);
    }

    @GetMapping("/reviews")
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/reviews/property/{propertyId}")
    public List<ReviewResponse> getReviewsByProperty(@PathVariable String propertyId) {
        return reviewRepository.findByPropertyId(propertyId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse toResponse(Review review) {
        // Feign Client를 사용하여 User Service로부터 유저 정보를 실시간으로 조회합니다.
        UserServiceClient.UserResponse user = userServiceClient.getUserById(review.getUserId());
        
        return ReviewResponse.builder()
                .id(review.getId())
                .propertyId(review.getPropertyId())
                .userId(review.getUserId())
                .userName(user.getName()) // 유저 서비스에서 가져온 닉네임!
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
