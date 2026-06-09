package org.micro.myservice.logistics.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.micro.myservice.logistics.event.EventPublisher;
import org.micro.myservice.logistics.event.LogisticsEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaEventStore implements EventPublisher {
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publish(LogisticsEvent event) {
        try {
            entityManager.persist(new OutboxEvent(
                    event.eventId(), event.aggregateId(), event.type().name(),
                    objectMapper.writeValueAsString(event), event.occurredAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize logistics event", exception);
        }
    }

    @Transactional(readOnly = true)
    public boolean wasProcessed(UUID eventId) {
        return entityManager.find(ProcessedEvent.class, eventId) != null;
    }

    @Transactional
    public void markProcessed(UUID eventId) {
        if (!wasProcessed(eventId)) {
            entityManager.persist(new ProcessedEvent(eventId, Instant.now()));
        }
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> findUnpublished(int batchSize) {
        return entityManager.createQuery(
                        "select event from OutboxEvent event where event.publishedAt is null order by event.createdAt",
                        OutboxEvent.class
                )
                .setMaxResults(batchSize)
                .getResultList();
    }

    @Transactional
    public void markPublished(UUID eventId) {
        OutboxEvent event = entityManager.find(OutboxEvent.class, eventId);
        if (event != null) {
            event.markPublished(Instant.now());
        }
    }
}
