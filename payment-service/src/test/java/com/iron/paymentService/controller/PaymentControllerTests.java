package com.iron.paymentService.controller;

import com.iron.controller.PaymentsController;
import com.iron.payment.model.PaymentRequest;
import com.iron.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTests {

    @Mock
    private PaymentService paymentService;

    @Mock
    private ServerWebExchange serverWebExchange;

    @InjectMocks
    private PaymentsController paymentsController;

    @BeforeEach
    void setUp() {
        paymentsController = new PaymentsController(paymentService);
    }

    @Test
    void getCurrentUserBalance_shouldReturnBalanceResponse() {
        BigDecimal balance = new BigDecimal("1000000.00");
        when(paymentService.getBalance()).thenReturn(Mono.just(balance));

        StepVerifier.create(paymentsController.getCurrentUserBalance(serverWebExchange))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getBalance()).isEqualTo(1000000.00);
                    assertThat(response.getBody().getCurrency()).isEqualTo("РУБ");
                })
                .verifyComplete();
    }

    @Test
    void getCurrentUserBalance_withZeroBalance_shouldReturnZeroBalance() {
        BigDecimal zeroBalance = BigDecimal.ZERO;
        when(paymentService.getBalance()).thenReturn(Mono.just(zeroBalance));

        StepVerifier.create(paymentsController.getCurrentUserBalance(serverWebExchange))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getBalance()).isEqualTo(0.0);
                    assertThat(response.getBody().getCurrency()).isEqualTo("РУБ");
                })
                .verifyComplete();
    }

    @Test
    void performPayment_withValidAmount_shouldReturnSuccessResponse() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(100.0);

        BigDecimal newBalance = new BigDecimal("999900.00");
        when(paymentService.performPayment(any(BigDecimal.class))).thenReturn(Mono.just(newBalance));

        StepVerifier.create(paymentsController.performPayment(Mono.just(paymentRequest), serverWebExchange))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
                    assertThat(response.getBody().getRemainingBalance()).isEqualTo(999900.00);
                })
                .verifyComplete();
    }

    @Test
    void performPayment_withDecimalAmount_shouldReturnSuccessResponse() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(123.45);

        BigDecimal newBalance = new BigDecimal("999876.55");
        when(paymentService.performPayment(any(BigDecimal.class))).thenReturn(Mono.just(newBalance));

        StepVerifier.create(paymentsController.performPayment(Mono.just(paymentRequest), serverWebExchange))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
                    assertThat(response.getBody().getRemainingBalance()).isEqualTo(999876.55);
                })
                .verifyComplete();
    }

    @Test
    void performPayment_withZeroAmount_shouldReturnError() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(0.0);

        when(paymentService.performPayment(any(BigDecimal.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Amount must be positive")));

        StepVerifier.create(paymentsController.performPayment(Mono.just(paymentRequest), serverWebExchange))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Amount must be positive"))
                .verify();
    }

    @Test
    void performPayment_withNegativeAmount_shouldReturnError() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(-50.0);

        when(paymentService.performPayment(any(BigDecimal.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Amount must be positive")));

        StepVerifier.create(paymentsController.performPayment(Mono.just(paymentRequest), serverWebExchange))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Amount must be positive"))
                .verify();
    }

    @Test
    void performPayment_withInsufficientFunds_shouldReturnError() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(2000000.0);

        when(paymentService.performPayment(any(BigDecimal.class)))
                .thenReturn(Mono.error(new RuntimeException("Insufficient funds")));

        StepVerifier.create(paymentsController.performPayment(Mono.just(paymentRequest), serverWebExchange))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Insufficient funds"))
                .verify();
    }

    @Test
    void performPayment_withExactBalance_shouldReturnZeroRemainingBalance() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(1000000.0);

        BigDecimal zeroBalance = BigDecimal.ZERO;
        when(paymentService.performPayment(any(BigDecimal.class))).thenReturn(Mono.just(zeroBalance));

        StepVerifier.create(paymentsController.performPayment(Mono.just(paymentRequest), serverWebExchange))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
                    assertThat(response.getBody().getRemainingBalance()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    @Test
    void performPayment_withLargeAmount_shouldHandleCorrectly() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(999999.99);

        BigDecimal newBalance = new BigDecimal("0.01");
        when(paymentService.performPayment(any(BigDecimal.class))).thenReturn(Mono.just(newBalance));

        StepVerifier.create(paymentsController.performPayment(Mono.just(paymentRequest), serverWebExchange))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
                    assertThat(response.getBody().getRemainingBalance()).isEqualTo(0.01);
                })
                .verifyComplete();
    }

    @Test
    void performPayment_withNullPaymentRequestMono_shouldPropagateError() {
        StepVerifier.create(paymentsController.performPayment(Mono.empty(), serverWebExchange))
                .expectError()
                .verify();
    }

    @Test
    void performPayment_serviceReturnsError_shouldPropagateError() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(100.0);

        when(paymentService.performPayment(any(BigDecimal.class)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        StepVerifier.create(paymentsController.performPayment(Mono.just(paymentRequest), serverWebExchange))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Service error"))
                .verify();
    }

    @Test
    void getCurrentUserBalance_serviceReturnsError_shouldPropagateError() {
        when(paymentService.getBalance())
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        StepVerifier.create(paymentsController.getCurrentUserBalance(serverWebExchange))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Service error"))
                .verify();
    }

    @Test
    void performPayment_withSmallAmount_shouldWorkCorrectly() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(0.01);

        BigDecimal newBalance = new BigDecimal("999999.99");
        when(paymentService.performPayment(any(BigDecimal.class))).thenReturn(Mono.just(newBalance));

        StepVerifier.create(paymentsController.performPayment(Mono.just(paymentRequest), serverWebExchange))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
                    assertThat(response.getBody().getRemainingBalance()).isEqualTo(999999.99);
                })
                .verifyComplete();
    }
}
