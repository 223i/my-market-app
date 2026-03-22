package com.iron.mymarket.service;

import com.iron.payment.client.api.DefaultApi;
import com.iron.payment.client.model.BalanceResponse;
import com.iron.payment.client.model.PaymentRequest;
import com.iron.payment.client.model.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentClientServiceTest {

    @Mock
    private DefaultApi paymentsApi;

    @InjectMocks
    private PaymentClientService paymentClientService;

    @BeforeEach
    void setUp() {
        lenient().when(paymentsApi.getCurrentUserBalance())
                .thenReturn(Mono.just(new BalanceResponse().balance(1000.0)));

        lenient().when(paymentsApi.performPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(new PaymentResponse().remainingBalance(500.0)));
    }

    @Test
    void getBalance_withAvailableService_shouldReturnBalance() {
        BalanceResponse expectedResponse = new BalanceResponse();
        expectedResponse.setBalance(1000.0);

        when(paymentsApi.getCurrentUserBalance())
                .thenReturn(Mono.just(expectedResponse));

        Mono<Double> result = paymentClientService.getBalance();

        StepVerifier.create(result)
                .expectNext(1000.0)
                .verifyComplete();

        verify(paymentsApi, times(1)).getCurrentUserBalance();
    }

    @Test
    void getBalance_withConnectionRefused_shouldThrowRuntimeException() {
        when(paymentsApi.getCurrentUserBalance())
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        Mono<Double> result = paymentClientService.getBalance();

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Сервис оплаты временно недоступен. Попробуйте позже.")
                )
                .verify();

        verify(paymentsApi, times(1)).getCurrentUserBalance();
    }

    @Test
    void pay_withValidAmount_shouldReturnRemainingBalance() {
        double amount = 500.0;
        PaymentResponse expectedResponse = new PaymentResponse();
        expectedResponse.setRemainingBalance(500.0);

        when(paymentsApi.performPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(expectedResponse));

        Mono<Double> result = paymentClientService.pay(amount);

        StepVerifier.create(result)
                .expectNext(500.0)
                .verifyComplete();

        verify(paymentsApi, times(1)).performPayment(any(PaymentRequest.class));
    }

    @Test
    void pay_withConnectionError_shouldThrowRuntimeException() {
        double amount = 500.0;
        when(paymentsApi.performPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        Mono<Double> result = paymentClientService.pay(amount);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Сервис оплаты временно недоступен. Попробуйте позже.")
                )
                .verify();

        verify(paymentsApi, times(1)).performPayment(any(PaymentRequest.class));
    }

    @Test
    void pay_withHostUnreachable_shouldThrowRuntimeException() {
        double amount = 500.0;
        when(paymentsApi.performPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("No route to host")));

        Mono<Double> result = paymentClientService.pay(amount);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Сервис оплаты временно недоступен. Попробуйте позже.")
                )
                .verify();

        verify(paymentsApi, times(1)).performPayment(any(PaymentRequest.class));
    }

    @Test
    void pay_withUnexpectedError_shouldPropagateOriginalError() {
        double amount = 500.0;
        when(paymentsApi.performPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        Mono<Double> result = paymentClientService.pay(amount);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Unexpected error")
                )
                .verify();

        verify(paymentsApi, times(1)).performPayment(any(PaymentRequest.class));
    }

    @Test
    void pay_withNullErrorMessage_shouldPropagateOriginalError() {
        double amount = 500.0;
        RuntimeException nullMessageError = new RuntimeException((String) null);
        when(paymentsApi.performPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.error(nullMessageError));

        Mono<Double> result = paymentClientService.pay(amount);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(paymentsApi, times(1)).performPayment(any(PaymentRequest.class));
    }
}
