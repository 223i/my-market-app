package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.User;
import com.iron.mymarket.dao.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class CustomReactiveOAuth2UserService extends DefaultReactiveOAuth2UserService {
    private final UserRepository userRepository;

    public CustomReactiveOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest)
                .flatMap(oauth2User -> {
                    String externalId = oauth2User.getAttribute("sub");
                    String email = oauth2User.getAttribute("email");
                    String username = oauth2User.getAttribute("preferred_username");

                    // 1. Ищем или создаем пользователя в H2
                    return userRepository.findByExternalId(externalId)
                            .switchIfEmpty(Mono.defer(() ->
                                    userRepository.save(new User(externalId, email, username))
                            ))
                            .map(userEntity -> {
                                // 2. Копируем атрибуты из Keycloak и добавляем наш internal_id
                                Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
                                attributes.put("internal_id", userEntity.getId());

                                // 3. Возвращаем новый объект пользователя с внедренным ID
                                return new DefaultOAuth2User(
                                        oauth2User.getAuthorities(),
                                        attributes,
                                        "preferred_username" // Имя атрибута для Principal.getName()
                                );
                            });
                });
    }
}
