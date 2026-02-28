package com.iron.mymarket.service;



import com.iron.payment.client.api.DefaultApi;
import com.iron.payment.client.model.BalanceResponse;
import com.iron.payment.client.model.PaymentRequest;
import com.iron.payment.client.model.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PaymentClientService {

    private final DefaultApi paymentsApi;

    public Mono<Double> getBalance() {
        return paymentsApi.getCurrentUserBalance()
                .map(BalanceResponse::getBalance);
    }

    public Mono<Double> pay(double amount) {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(amount);

        return paymentsApi.performPayment(request)
                .map(PaymentResponse::getRemainingBalance);

    }
}