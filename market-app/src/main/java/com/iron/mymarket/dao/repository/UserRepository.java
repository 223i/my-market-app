package com.iron.mymarket.dao.repository;

import com.iron.mymarket.dao.entities.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByExternalId(String externalId);
}
