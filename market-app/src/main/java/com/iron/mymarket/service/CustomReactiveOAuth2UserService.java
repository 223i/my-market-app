package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.User;
import com.iron.mymarket.dao.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CustomReactiveOAuth2UserService extends DefaultReactiveOAuth2UserService {
    private final UserRepository userRepository;

    public CustomReactiveOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) {
        // 1. Сначала получаем пользователя от Keycloak
        return super.loadUser(userRequest)
                .flatMap(oauth2User -> {
                    // 2. Извлекаем данные из токена (claims)
                    String externalId = oauth2User.getAttribute("sub"); // Уникальный ID в Keycloak
                    String email = oauth2User.getAttribute("email");
                    String username = oauth2User.getAttribute("preferred_username");

                    // 3. Синхронизируем с нашей БД H2
                    return userRepository.findByExternalId(externalId)
                            .flatMap(existingUser -> {
                                // Если пользователь есть, можно обновить email/username если они изменились
                                existingUser.setEmail(email);
                                existingUser.setUsername(username);
                                return userRepository.save(existingUser);
                            })
                            .switchIfEmpty(
                                    // Если пользователя нет — создаем новую запись
                                    userRepository.save(new User(externalId, email, username))
                            )
                            // Возвращаем исходного oauth2User, чтобы Spring Security продолжил работу
                            .thenReturn(oauth2User);
                });
    }
}
