package org.micro.myservice.notification.repository;

import java.util.UUID;
import org.micro.myservice.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
