package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.User;
import com.iron.mymarket.dao.repository.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> findByExternalId(String id){
        return userRepository.findByExternalId(id);
    }
}
