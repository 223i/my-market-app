package com.iron.mymarket.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

//    @ExceptionHandler(ServerWebInputException.class)
//    public Mono<ServerResponse> handleBadRequest(ServerWebInputException ex) {
//        return ServerResponse.badRequest().build();
//    }

//    @ExceptionHandler(Throwable.class)
//    public Mono<ServerResponse> handleOther(Throwable ex) {
//        return ServerResponse
//                .status(500)
//                .contentType(MediaType.TEXT_HTML)
//                .render("error");
//    }
}
