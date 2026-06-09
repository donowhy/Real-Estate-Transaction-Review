package org.micro.myservice.logistics.event;

public interface EventPublisher {
    void publish(LogisticsEvent event);
}
