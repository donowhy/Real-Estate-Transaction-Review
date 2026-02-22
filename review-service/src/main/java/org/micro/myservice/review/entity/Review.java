package org.micro.myservice.review.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String propertyId; // 부동산 식별값 (예: 아파트 코드)

    @Column(nullable = false)
    private Long userId; // 작성자 ID (User Service와 연계)

    @Column(nullable = false)
    private Integer rating; // 평점 (1~5)

    @Column(nullable = false, length = 1000)
    private String content; // 후기 내용

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt; // 작성 시간

    @Builder
    public Review(String propertyId, Long userId, Integer rating, String content) {
        this.propertyId = propertyId;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
    }
}
