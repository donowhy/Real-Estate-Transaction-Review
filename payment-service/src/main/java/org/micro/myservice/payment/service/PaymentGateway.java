package org.micro.myservice.payment.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {
    void approve(UUID orderId, BigDecimal amount);
    void cancel(UUID orderId, BigDecimal amount);
}
