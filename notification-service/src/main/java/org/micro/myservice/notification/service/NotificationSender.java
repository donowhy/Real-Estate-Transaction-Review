package org.micro.myservice.notification.service;

import org.micro.myservice.logistics.event.LogisticsEvent;

public interface NotificationSender {
    void send(LogisticsEvent event);
}
