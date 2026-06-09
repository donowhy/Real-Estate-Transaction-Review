package org.micro.myservice.payment.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LocalPaymentGateway implements PaymentGateway {
    @Override
    public void approve(UUID orderId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentRejectedException("Payment amount must be positive");
        }
    }

    @Override
    public void cancel(UUID orderId, BigDecimal amount) {
        // Replace this adapter when an external payment provider is connected.
    }
}
