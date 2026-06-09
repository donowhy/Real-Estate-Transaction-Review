package org.micro.analysis.domain.news.repository;

import org.micro.analysis.domain.news.entity.NewsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface NewsEventRepository extends JpaRepository<NewsEvent, Long> {
    List<NewsEvent> findAllByCreatedAtAfter(LocalDateTime time);
}
