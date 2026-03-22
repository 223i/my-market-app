package com.iron.mymarket.configuration;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException e) {
        String path = exchange.getRequest().getPath().value();
        
        if (path.startsWith("/cart/") || path.startsWith("/orders/")) {
            exchange.getResponse().getHeaders().setLocation(java.net.URI.create("/auth/login"));
            exchange.getResponse().setStatusCode(HttpStatus.FOUND);
            return exchange.getResponse().setComplete();
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
