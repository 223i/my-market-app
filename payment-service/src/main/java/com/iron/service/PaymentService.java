package com.iron.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PaymentService {

    private final AtomicReference<BigDecimal> balance;


    public PaymentService(@Value("${payment.initial.balance:10000}") BigDecimal balance) {
        this.balance = new AtomicReference<>(balance);;
    }


    public Mono<BigDecimal> getBalance() {
        return Mono.just(balance.get());
    }

    public Mono<BigDecimal> performPayment(BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be positive"));
        }

        while (true) {
            BigDecimal current = balance.get();

            if (current.compareTo(amount) < 0) {
                return Mono.error(new RuntimeException("Insufficient funds"));
            }

            BigDecimal updated = current.subtract(amount);

            // atomic CAS update
            if (balance.compareAndSet(current, updated)) {
                return Mono.just(updated);
            }
        }
    }
}
