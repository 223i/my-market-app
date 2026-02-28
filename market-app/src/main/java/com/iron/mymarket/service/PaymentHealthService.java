package com.iron.mymarket.service;

import com.iron.payment.client.api.DefaultApi;
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
    private volatile boolean isPaymentServiceAvailable = true;
    private volatile long lastCheckTime = 0;
    private static final long CHECK_INTERVAL_MS = 30000; // 30 секунд
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    public Mono<Boolean> isPaymentServiceAvailable() {
        long currentTime = System.currentTimeMillis();
        
        // Если последняя проверка была недавно, вернем кешированный результат
        if (currentTime - lastCheckTime < CHECK_INTERVAL_MS) {
            return Mono.just(isPaymentServiceAvailable);
        }

        return paymentApi.getCurrentUserBalance()
                .timeout(TIMEOUT)
                .map(response -> {
                    isPaymentServiceAvailable = true;
                    lastCheckTime = currentTime;
                    log.debug("Payment service is available");
                    return true;
                })
                .onErrorResume(e -> {
                    isPaymentServiceAvailable = false;
                    lastCheckTime = currentTime;
                    String errorMessage = e.getMessage();
                    
                    // Проверяем на конкретные ошибки соединения
                    if (errorMessage != null && (
                        errorMessage.contains("Connection refused") ||
                        errorMessage.contains("localhost:8081") ||
                        errorMessage.contains("No route to host") ||
                        errorMessage.contains("Connection timeout") ||
                        errorMessage.contains("ConnectException"))) {
                        log.warn("Payment service connection failed: {}", errorMessage);
                    } else {
                        log.warn("Payment service is unavailable: {}", errorMessage);
                    }
                    
                    return Mono.just(false);
                });
    }

    public void forceCheck() {
        lastCheckTime = 0; // Сбрасываем время кеша для принудительной проверки
    }
}
