package org.micro.myservice.review.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private String propertyId;
    private Long userId;
    private Integer rating;
    private String content;
}
