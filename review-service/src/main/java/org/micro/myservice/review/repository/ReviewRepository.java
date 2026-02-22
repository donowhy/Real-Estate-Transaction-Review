package org.micro.myservice.review.repository;

import org.micro.myservice.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 특정 부동산(아파트 등)에 대한 후기 목록 조회
    List<Review> findByPropertyId(String propertyId);
    
    // 특정 유저가 쓴 후기 목록 조회
    List<Review> findByUserId(Long userId);
}
