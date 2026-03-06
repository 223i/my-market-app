package com.iron.mymarket.service;

import com.iron.payment.client.api.DefaultApi;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentHealthService {

    private final DefaultApi paymentApi;
    private final CircuitBreakerRegistry registry;

    public Mono<Boolean> isPaymentServiceAvailable() {
        var circuitBreaker = registry.circuitBreaker("paymentService");

        return paymentApi.getCurrentUserBalance()
                .timeout(Duration.ofSeconds(3))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .map(response -> true)
                .onErrorResume(e -> {
                    log.warn("Payment check failed: {}", e.getMessage());
                    return Mono.just(false);
                });

    }
}
