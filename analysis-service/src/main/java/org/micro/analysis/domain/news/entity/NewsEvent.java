package org.micro.analysis.domain.news.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsEvent {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String mainTitle;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<NewsArticle> articles = new ArrayList<>();
}
