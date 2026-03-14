package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.User;
import com.iron.mymarket.dao.repository.UserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@Primary
public class CustomOidcUserService extends OidcReactiveOAuth2UserService {
    private final UserRepository userRepository;

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<OidcUser> loadUser(OidcUserRequest userRequest) {
        return super.loadUser(userRequest)
                .flatMap(oidcUser -> {
                    String sub = oidcUser.getSubject();
                    String email = oidcUser.getAttribute("email");
                    String username = oidcUser.getPreferredUsername();

                    return userRepository.findByExternalId(sub)
                            .switchIfEmpty(Mono.defer(() ->
                                    userRepository.save(new User(sub, email, username))
                            ))
                            .map(userEntity -> {
                                Map<String, Object> attributes = new HashMap<>(oidcUser.getAttributes());
                                attributes.put("internal_id", userEntity.getId());

                                return new DefaultOidcUser(
                                        oidcUser.getAuthorities(),
                                        oidcUser.getIdToken(),
                                        oidcUser.getUserInfo(),
                                        "preferred_username"
                                );
                            });
                });
    }
}