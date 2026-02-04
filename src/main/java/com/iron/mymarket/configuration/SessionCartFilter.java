package com.iron.mymarket.configuration;

import com.iron.mymarket.dao.repository.CartStorage;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class SessionCartFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return  exchange.getSession()
                .doOnNext(s -> s.getAttributes().putIfAbsent("cart", new CartStorage()))
                .then(chain.filter(exchange));

    }
}
