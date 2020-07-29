package com.example.callee.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;


@EnableWebFluxSecurity
@Slf4j
public class SecurityConfig
{

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String oauthUrl;

    private final WebClient.Builder authServerWebClientBuilder;

    private WebClient webClient;

    /**
     * Constructor SecurityConfig
     *
     * @param authServerWebClientBuilder
     */
    public SecurityConfig(WebClient.Builder authServerWebClientBuilder)
    {
        this.authServerWebClientBuilder = authServerWebClientBuilder;
    }

    @PostConstruct
    public void init()
    {
        this.webClient = authServerWebClientBuilder.build();
    }

    @Bean
    protected SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http)
    {
        http.authorizeExchange()

        // .pathMatchers(HttpMethod.GET, "/actuator/**").permitAll()
        .anyExchange().authenticated().and().oauth2ResourceServer().jwt();

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder()
    {
        return NimbusReactiveJwtDecoder.withJwkSetUri(oauthUrl)
                                       .webClient(webClient)
                                       .build();
    }

}
