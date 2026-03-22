package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.User;
import com.iron.mymarket.dao.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomOidcUserService customOidcUserService;
    private OidcUser mockOidcUser;
    private OidcUserRequest mockUserRequest;

    @BeforeEach
    void setUp() {
        customOidcUserService = new CustomOidcUserService(userRepository);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-subject");
        claims.put("email", "test@example.com");
        claims.put("preferred_username", "testuser");

        OidcIdToken idToken = new OidcIdToken("test-token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);
        
        mockOidcUser = new DefaultOidcUser(
            Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
            idToken,
            userInfo,
            "preferred_username"
        );

        mockUserRequest = createMockUserRequest();
    }

    private OidcUserRequest createMockUserRequest() {
        org.springframework.security.oauth2.client.registration.ClientRegistration clientRegistration = 
            org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId("keycloak")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/keycloak")
                .scope("openid", "profile", "email")
                .authorizationUri("http://test-keycloak:8080/realms/test-realm/protocol/openid-connect/auth")
                .tokenUri("http://test-keycloak:8080/realms/test-realm/protocol/openid-connect/token")
                .userInfoUri("http://test-keycloak:8080/realms/test-realm/protocol/openid-connect/userinfo")
                .jwkSetUri("http://test-keycloak:8080/realms/test-realm/protocol/openid-connect/certs")
                .issuerUri("http://test-keycloak:8080/realms/test-realm")
                .build();

        org.springframework.security.oauth2.core.OAuth2AccessToken accessToken = 
            new org.springframework.security.oauth2.core.OAuth2AccessToken(
                org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OidcUserRequest(clientRegistration, accessToken, mockOidcUser.getIdToken());
    }

    @Test
    void testLoadUser_NewUser_CreatesUserInDatabase() {
        User newUser = new User("test-subject", "test@example.com", "testuser");
        newUser.setId(1L);

        when(userRepository.findByExternalId("test-subject"))
                .thenReturn(Mono.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(newUser));

        StepVerifier.create(customOidcUserService.loadUser(mockUserRequest))
                .assertNext(oidcUser -> {
                    assertNotNull(oidcUser);
                    assertEquals("test-subject", oidcUser.getSubject());
                    assertEquals("testuser", oidcUser.getPreferredUsername());
                    assertEquals("test@example.com", oidcUser.getAttribute("email"));
                })
                .verifyComplete();

        verify(userRepository).findByExternalId("test-subject");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testLoadUser_ExistingUser_DoesNotCreateNewUser() {
        User existingUser = new User("test-subject", "test@example.com", "testuser");
        existingUser.setId(1L);

        when(userRepository.findByExternalId("test-subject"))
                .thenReturn(Mono.just(existingUser));

        StepVerifier.create(customOidcUserService.loadUser(mockUserRequest))
                .assertNext(oidcUser -> {
                    assertNotNull(oidcUser);
                    assertEquals("test-subject", oidcUser.getSubject());
                })
                .verifyComplete();

        verify(userRepository).findByExternalId("test-subject");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoadUser_DatabaseError_PropagatesError() {
        when(userRepository.findByExternalId("test-subject"))
                .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

        StepVerifier.create(customOidcUserService.loadUser(mockUserRequest))
                .expectError(RuntimeException.class)
                .verify();

        verify(userRepository).findByExternalId("test-subject");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoadUser_SaveOperationError_PropagatesError() {
        when(userRepository.findByExternalId("test-subject"))
                .thenReturn(Mono.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.error(new RuntimeException("Save operation failed")));

        StepVerifier.create(customOidcUserService.loadUser(mockUserRequest))
                .expectError(RuntimeException.class)
                .verify();

        verify(userRepository).findByExternalId("test-subject");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testLoadUser_UserAttributesMapping() {
        User existingUser = new User("test-subject", "test@example.com", "testuser");
        existingUser.setId(123L);

        when(userRepository.findByExternalId("test-subject"))
                .thenReturn(Mono.just(existingUser));

        StepVerifier.create(customOidcUserService.loadUser(mockUserRequest))
                .assertNext(oidcUser -> {
                    assertNotNull(oidcUser);
                    assertEquals("test-subject", oidcUser.getSubject());
                    assertEquals("testuser", oidcUser.getPreferredUsername());
                    assertEquals("test@example.com", oidcUser.getAttribute("email"));
                    
                    Map<String, Object> attributes = oidcUser.getAttributes();
                    assertNotNull(attributes);
                    assertEquals("test-subject", attributes.get("sub"));
                    assertEquals("test@example.com", attributes.get("email"));
                    assertEquals("testuser", attributes.get("preferred_username"));
                })
                .verifyComplete();
    }

    @Test
    void testLoadUser_MissingEmailAttribute_HandlesGracefully() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-subject");
        claims.put("preferred_username", "testuser");

        OidcIdToken idToken = new OidcIdToken("test-token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);
        
        OidcUser oidcUserWithoutEmail = new DefaultOidcUser(
            Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
            idToken,
            userInfo,
            "preferred_username"
        );

        User newUser = new User("test-subject", null, "testuser");
        newUser.setId(1L);

        when(userRepository.findByExternalId("test-subject"))
                .thenReturn(Mono.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(newUser));

        StepVerifier.create(customOidcUserService.loadUser(mockUserRequest))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals("test-subject", result.getSubject());
                    assertEquals("testuser", result.getPreferredUsername());
                })
                .verifyComplete();
    }

    @Test
    void testLoadUser_ConcurrentRequests_HandlesCorrectly() {
        User existingUser = new User("test-subject", "test@example.com", "testuser");
        existingUser.setId(1L);

        when(userRepository.findByExternalId("test-subject"))
                .thenReturn(Mono.just(existingUser));

        List<Mono<OidcUser>> requests = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            requests.add(customOidcUserService.loadUser(mockUserRequest));
        }

        StepVerifier.create(Mono.zip(requests, objects -> objects))
                .assertNext(results -> {
                    assertEquals(3, results.length);
                    for (Object result : results) {
                        OidcUser user = (OidcUser) result;
                        assertEquals("test-subject", user.getSubject());
                    }
                })
                .verifyComplete();

        verify(userRepository, times(3)).findByExternalId("test-subject");
    }

    @Test
    void testLoadUser_VerifyUserCreationParameters() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        
        when(userRepository.findByExternalId("test-subject"))
                .thenReturn(Mono.empty());
        when(userRepository.save(userCaptor.capture()))
                .thenReturn(Mono.just(new User("test-subject", "test@example.com", "testuser")));

        StepVerifier.create(customOidcUserService.loadUser(mockUserRequest))
                .expectNextCount(1)
                .verifyComplete();

        User savedUser = userCaptor.getValue();
        assertEquals("test-subject", savedUser.getExternalId());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("testuser", savedUser.getUsername());
    }
}
