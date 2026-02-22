package org.micro.myservice.review.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private String propertyId;
    private Long userId;
    private String userName; // User Service에서 가져온 정보!
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
