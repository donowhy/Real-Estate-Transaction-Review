package org.micro.myservice.logistics.messaging;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.micro.myservice.logistics.persistence.JpaEventStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRelay {
    private final JpaEventStore eventStore;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${logistics.kafka.topic:logistics.events}")
    private String topic;
    @Value("${logistics.outbox.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${logistics.outbox.fixed-delay:1000}")
    public void publishPendingEvents() {
        eventStore.findUnpublished(batchSize).forEach(event -> {
            try {
                kafkaTemplate.send(topic, event.getEventId().toString(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
                eventStore.markPublished(event.getEventId());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while publishing " + event.getEventId(), exception);
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to publish outbox event " + event.getEventId(), exception);
            }
        });
    }
}
