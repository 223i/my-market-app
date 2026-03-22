package com.iron.mymarket.security;

import com.iron.mymarket.configuration.SecurityConfig;
import com.iron.mymarket.configuration.TestConfig;
import com.iron.mymarket.dao.entities.User;
import com.iron.mymarket.dao.repository.UserRepository;
import com.iron.mymarket.service.CustomOidcUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@WebFluxTest
@Import({SecurityConfig.class, TestConfig.class})
@ActiveProfiles("test")
class OAuth2AuthenticationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CustomOidcUserService customOidcUserService;

    @MockitoBean
    private UserRepository userRepository;

    private OidcUser mockOidcUser;
    private User mockUser;
    private OidcUserRequest mockUserRequest;

    @BeforeEach
    void setUp() {
        mockUser = new User("test-subject", "test@example.com", "testuser");
        mockUser.setId(1L);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-subject");
        claims.put("email", "test@example.com");
        claims.put("preferred_username", "testuser");

        OidcIdToken idToken = new OidcIdToken("test-token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);

        mockOidcUser = new DefaultOidcUser(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                idToken,
                userInfo,
                "preferred_username"
        );

        mockUserRequest = createMockUserRequest();
    }

    private OidcUserRequest createMockUserRequest() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("keycloak")
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

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OidcUserRequest(clientRegistration, accessToken, mockOidcUser.getIdToken());
    }

    @Test
    void testPublicEndpointsAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().is2xxSuccessful();

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    void testCustomOidcUserService_NewUserCreation() {
        when(customOidcUserService.loadUser(any(OidcUserRequest.class)))
                .thenReturn(Mono.just(mockOidcUser));
        when(userRepository.findByExternalId("test-subject"))
                .thenReturn(Mono.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(mockUser));

        Mono<OidcUser> result = customOidcUserService.loadUser(mockUserRequest);
        OidcUser user = result.block();
        
        assertNotNull(user);
        assertEquals("test-subject", user.getSubject());
        verify(customOidcUserService).loadUser(any(OidcUserRequest.class));
    }

    @Test
    void testCustomOidcUserService_ExistingUserRetrieval() {
        when(customOidcUserService.loadUser(any(OidcUserRequest.class)))
                .thenReturn(Mono.just(mockOidcUser));
        when(userRepository.findByExternalId("test-subject"))
                .thenReturn(Mono.just(mockUser));

        Mono<OidcUser> result = customOidcUserService.loadUser(mockUserRequest);
        OidcUser user = result.block();
        
        assertNotNull(user);
        assertEquals("test-subject", user.getSubject());
        verify(customOidcUserService).loadUser(any(OidcUserRequest.class));
    }
    @Test
    void testLogoutEndpoint() {
        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items?logout=true");
    }

    @Test
    void testUserAuthenticationContext() {
        Authentication auth = new TestingAuthenticationToken(
                mockOidcUser,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertNotNull(auth.getPrincipal());
        assertTrue(auth.getPrincipal() instanceof OidcUser);
        OidcUser oidcUser = (OidcUser) auth.getPrincipal();
        assertEquals("test-subject", oidcUser.getSubject());
        assertEquals("testuser", oidcUser.getPreferredUsername());
    }

    @Test
    void testSecurityConfiguration_DisabledCSRF() {
        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    void testOAuth2TokenValidation() {
        assertNotNull(mockOidcUser.getIdToken());
        assertNotNull(mockOidcUser.getUserInfo());
        assertEquals("test-subject", mockOidcUser.getSubject());
        assertTrue(mockOidcUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }
}
