package org.micro.analysis.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.micro.analysis.domain.news.entity.NewsArticle;
import org.micro.analysis.domain.news.entity.NewsEvent;
import org.micro.analysis.domain.news.repository.NewsEventRepository;
import org.micro.analysis.domain.news.services.GeminiService;
import org.micro.analysis.domain.news.services.dto.NewsMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@Slf4j
public class AnalysisConsumer {

    private final NewsEventRepository eventRepository;
    private final GeminiService geminiService;

    public AnalysisConsumer(NewsEventRepository eventRepository, GeminiService geminiService) {
        this.eventRepository = eventRepository;
        this.geminiService = geminiService;
    }

    @KafkaListener(topics = "raw-news-topic", groupId = "analysis-group")
    @Transactional
    public void consume(NewsMessage news) {
        // [중요 로그] Kafka에서 데이터를 받았는지 확인
        log.info("🔥 [KAFKA RECEIVED] Source: {}, Title: {}", news.getSource(), news.getTitle());

        try {
            List<NewsEvent> recentEvents = eventRepository.findAllByCreatedAtAfter(LocalDateTime.now().minusHours(6));
            log.info("Checking matching events... (Current count in DB: {})", recentEvents.size());
            
            Long matchedEventId = geminiService.findMatchingEvent(news.getTitle(), recentEvents);

            if (matchedEventId != null) {
                log.info("📌 Match found! Appending to Event ID: {}", matchedEventId);
                NewsEvent event = eventRepository.findById(matchedEventId).orElseThrow();
                addArticleToEvent(event, news);
            } else {
                log.info("✨ No match. Creating NEW Event.");
                createNewEvent(news);
            }
        } catch (Exception e) {
            log.error("❌ Error during analysis: {}", e.getMessage(), e);
        }
    }

    private void addArticleToEvent(NewsEvent event, NewsMessage news) {
        String perspective = geminiService.analyzePerspective(news.getTitle(), news.getSource(), event.getSummary());
        
        NewsArticle article = NewsArticle.builder()
                .event(event)
                .sourceName(news.getSource())
                .originalTitle(news.getTitle())
                .perspective(perspective)
                .url(news.getUrl())
                .build();
        
        if (event.getArticles() == null) event.setArticles(new ArrayList<>());
        event.getArticles().add(article);
        
        String updatedSummary = geminiService.updateGlobalSummary(event.getSummary(), news.getTitle(), news.getSource());
        event.setSummary(updatedSummary);
        
        eventRepository.save(event);
    }

    private void createNewEvent(NewsMessage news) {
        String summary = geminiService.generateInitialSummary(news.getTitle());
        NewsEvent event = NewsEvent.builder()
                .mainTitle(news.getTitle())
                .summary(summary)
                .createdAt(LocalDateTime.now())
                .articles(new ArrayList<>())
                .build();
        
        NewsArticle article = NewsArticle.builder()
                .event(event)
                .sourceName(news.getSource())
                .originalTitle(news.getTitle())
                .perspective(summary)
                .url(news.getUrl())
                .build();
        
        event.getArticles().add(article);
        eventRepository.save(event);
    }
}
