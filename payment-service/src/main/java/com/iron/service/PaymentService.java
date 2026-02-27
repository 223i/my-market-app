package com.iron.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {

    private final Map<String, Double> balances = new ConcurrentHashMap<>();

    public PaymentService() {
        balances.put("user-1", 100.0);
        balances.put("user-2", 250.0);
    }

    public Mono<Double> getBalance(String userId) {
        return Mono.justOrEmpty(balances.get(userId));
    }

    public Mono<Double> performPayment(String userId, double amount) {

        Double balance = balances.get(userId);

        if (balance == null) {
            return Mono.error(new RuntimeException("User not found"));
        }

        if (balance < amount) {
            return Mono.error(new RuntimeException("Insufficient funds"));
        }

        double newBalance = balance - amount;
        balances.put(userId, newBalance);

        return Mono.just(newBalance);
    }
}
