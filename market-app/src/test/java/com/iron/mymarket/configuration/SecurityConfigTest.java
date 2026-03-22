package com.iron.mymarket.configuration;

import com.iron.mymarket.service.CustomOidcUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest
@Import({SecurityConfig.class, TestConfig.class})
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CustomOidcUserService customOidcUserService;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    @MockitoBean
    private com.iron.mymarket.configuration.CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Test
    void testPublicEndpoints_AccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().is2xxSuccessful();

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().is2xxSuccessful();

        webTestClient.get()
                .uri("/auth/login")
                .exchange()
                .expectStatus().is2xxSuccessful();

        webTestClient.get()
                .uri("/login/oauth2/code/keycloak")
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    void testStaticResources_AccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/css/style.css")
                .exchange()
                .expectStatus().is4xxClientError();

        webTestClient.get()
                .uri("/js/app.js")
                .exchange()
                .expectStatus().is4xxClientError();

        webTestClient.get()
                .uri("/img/logo.png")
                .exchange()
                .expectStatus().is4xxClientError();

        webTestClient.get()
                .uri("/static/file.txt")
                .exchange()
                .expectStatus().is4xxClientError();

        webTestClient.get()
                .uri("/public/file.txt")
                .exchange()
                .expectStatus().is4xxClientError();

        webTestClient.get()
                .uri("/resources/file.txt")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void testLogoutEndpoint_RedirectsCorrectly() {
        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items?logout=true");
    }

    @Test
    void testCSRF_Disabled() {
        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().is3xxRedirection();
    }
}
