package com.iron.controller;

import com.iron.payment.api.PaymentsApi;
import com.iron.payment.model.BalanceResponse;
import com.iron.payment.model.PaymentRequest;
import com.iron.payment.model.PaymentResponse;
import com.iron.service.PaymentService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class PaymentsController implements PaymentsApi {

    private final PaymentService paymentService;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getCurrentUserBalance(
            @Parameter(hidden = true) final ServerWebExchange exchange) {

        return paymentService.getBalance()
                .map(balance -> {
                    BalanceResponse response = new BalanceResponse();
                    response.setBalance(balance.doubleValue());
                    response.setCurrency("РУБ");

                    return ResponseEntity.ok(response);
                });
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> performPayment(
            @Parameter(name = "PaymentRequest", description = "", required = true) @Valid @RequestBody Mono<PaymentRequest> paymentRequest,
            @Parameter(hidden = true) final ServerWebExchange exchange) {
        return paymentRequest.flatMap(req ->
                paymentService.performPayment(BigDecimal.valueOf(req.getAmount()))
                        .map(newBalance -> {
                            PaymentResponse response = new PaymentResponse();
                            response.setStatus("SUCCESS");
                            response.setRemainingBalance(newBalance.doubleValue());

                            return ResponseEntity.ok(response);
                        })
        );
    }

}
