package org.micro.myservice.payment.repository;

import java.util.UUID;
import org.micro.myservice.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
