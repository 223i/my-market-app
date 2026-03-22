package com.iron.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
public class AuthController {

    @GetMapping("/auth/login")
    public Mono<Rendering> login(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout) {
        return Mono.just(Rendering.view("login")
                .modelAttribute("error", error != null)
                .modelAttribute("logout", logout != null)
                .build());
    }
}
