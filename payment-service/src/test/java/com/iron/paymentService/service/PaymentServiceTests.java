package com.iron.paymentService.service;

import com.iron.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }

    @Test
    void getBalance_shouldReturnInitialBalance() {
        StepVerifier.create(paymentService.getBalance())
                .expectNext(new BigDecimal("1000000.00"))
                .verifyComplete();
    }

    @Test
    void performPayment_withValidAmount_shouldReturnUpdatedBalance() {
        BigDecimal paymentAmount = new BigDecimal("100.00");
        BigDecimal expectedBalance = new BigDecimal("999900.00");

        StepVerifier.create(paymentService.performPayment(paymentAmount))
                .expectNext(expectedBalance)
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance())
                .expectNext(expectedBalance)
                .verifyComplete();
    }

    @Test
    void performPayment_withNullAmount_shouldReturnError() {
        StepVerifier.create(paymentService.performPayment(null))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Amount must be positive"))
                .verify();
    }

    @Test
    void performPayment_withZeroAmount_shouldReturnError() {
        BigDecimal zeroAmount = BigDecimal.ZERO;

        StepVerifier.create(paymentService.performPayment(zeroAmount))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Amount must be positive"))
                .verify();
    }

    @Test
    void performPayment_withNegativeAmount_shouldReturnError() {
        BigDecimal negativeAmount = new BigDecimal("-50.00");

        StepVerifier.create(paymentService.performPayment(negativeAmount))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Amount must be positive"))
                .verify();
    }

    @Test
    void performPayment_withAmountExceedingBalance_shouldReturnError() {
        BigDecimal excessiveAmount = new BigDecimal("2000000.00");

        StepVerifier.create(paymentService.performPayment(excessiveAmount))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Insufficient funds"))
                .verify();
    }

    @Test
    void performPayment_withExactBalance_shouldSucceed() {
        BigDecimal exactBalanceAmount = new BigDecimal("1000000.00");

        StepVerifier.create(paymentService.performPayment(exactBalanceAmount))
                .assertNext(actual -> assertThat(actual).isEqualByComparingTo(BigDecimal.ZERO))
                .verifyComplete();
    }

    @Test
    void performPayment_multiplePayments_shouldUpdateBalanceCorrectly() {
        BigDecimal firstPayment = new BigDecimal("1000.00");
        BigDecimal secondPayment = new BigDecimal("500.00");
        BigDecimal thirdPayment = new BigDecimal("200.50");

        BigDecimal expectedAfterFirst = new BigDecimal("999000.00");
        BigDecimal expectedAfterSecond = new BigDecimal("998500.00");
        BigDecimal expectedAfterThird = new BigDecimal("998299.50");

        StepVerifier.create(paymentService.performPayment(firstPayment))
                .expectNext(expectedAfterFirst)
                .verifyComplete();

        StepVerifier.create(paymentService.performPayment(secondPayment))
                .expectNext(expectedAfterSecond)
                .verifyComplete();

        StepVerifier.create(paymentService.performPayment(thirdPayment))
                .expectNext(expectedAfterThird)
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance())
                .expectNext(expectedAfterThird)
                .verifyComplete();
    }

    @Test
    void performPayment_withDecimalAmount_shouldWorkCorrectly() {
        BigDecimal decimalPayment = new BigDecimal("123.45");
        BigDecimal expectedBalance = new BigDecimal("999876.55");

        StepVerifier.create(paymentService.performPayment(decimalPayment))
                .expectNext(expectedBalance)
                .verifyComplete();
    }
}
