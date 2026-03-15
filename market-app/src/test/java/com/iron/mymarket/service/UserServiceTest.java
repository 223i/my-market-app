package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.User;
import com.iron.mymarket.dao.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("user123", "test@example.com", "Test User");
    }

    @Test
    void findByExternalId_withExistingUser_shouldReturnUser() {
        // Given
        String externalId = "user123";
        when(userRepository.findByExternalId(externalId)).thenReturn(Mono.just(testUser));

        // When
        Mono<User> result = userService.findByExternalId(externalId);

        // Then
        StepVerifier.create(result)
                .expectNext(testUser)
                .verifyComplete();

        verify(userRepository, times(1)).findByExternalId(externalId);
    }

    @Test
    void findByExternalId_withNonExistingUser_shouldReturnEmpty() {
        // Given
        String externalId = "nonexistent";
        when(userRepository.findByExternalId(externalId)).thenReturn(Mono.empty());

        // When
        Mono<User> result = userService.findByExternalId(externalId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository, times(1)).findByExternalId(externalId);
    }

    @Test
    void findByExternalId_withNullExternalId_shouldReturnEmpty() {
        // Given
        String externalId = null;
        when(userRepository.findByExternalId(externalId)).thenReturn(Mono.empty());

        // When
        Mono<User> result = userService.findByExternalId(externalId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository, times(1)).findByExternalId(externalId);
    }
}
