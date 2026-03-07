package com.iron.mymarket.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    public Mono<Boolean> set(String key, Object value) {
        return reactiveRedisTemplate.opsForValue()
                .set(key, value)
                .doOnSuccess(success -> {
                    if (success) {
                        log.debug("Cache set successfully for key: {}", key);
                    } else {
                        log.warn("Failed to set cache for key: {}", key);
                    }
                });
    }

    public Mono<Boolean> setWithExpiration(String key, Object value, Duration duration) {
        return reactiveRedisTemplate.opsForValue()
                .set(key, value, duration)
                .doOnSuccess(success -> {
                    if (success) {
                        log.debug("Cache set with expiration for key: {}, duration: {}", key, duration);
                    } else {
                        log.warn("Failed to set cache with expiration for key: {}", key);
                    }
                });
    }

    public Mono<Object> get(String key) {
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .doOnNext(value -> {
                    if (value != null) {
                        log.debug("Cache hit for key: {}", key);
                    } else {
                        log.debug("Cache miss for key: {}", key);
                    }
                });
    }

    public Mono<Boolean> delete(String key) {
        return reactiveRedisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnSuccess(deleted -> {
                    if (deleted) {
                        log.debug("Cache deleted for key: {}", key);
                    } else {
                        log.debug("Cache key not found for deletion: {}", key);
                    }
                });
    }

    public Mono<Boolean> exists(String key) {
        return reactiveRedisTemplate.hasKey(key)
                .doOnNext(exists -> log.debug("Cache exists check for key: {}, result: {}", key, exists));
    }

    public Mono<String> generateKey(String prefix, String... parts) {
        return Mono.just(String.join(":", parts.length > 0 ? parts : new String[]{prefix}));
    }


    public Mono<Long> deleteByPattern(String pattern) {
        return reactiveRedisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(pattern)
                        .build()
        ).flatMap(this::delete)
                .count();
    }
}