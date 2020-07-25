package com.example.caller.scheduler;


import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;


/**
 * Class Scheduler
 */
@Configuration
@EnableScheduling
@Slf4j
public class Scheduler
{

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.build();
    }

    @Scheduled(fixedRate = 10000L)
    public void callApi()
    {
        log.info("before callApi");

        String body = this.webClient.get().uri("https://localhost:8000/api")

        // .attributes(clientRegistrationId("keycloak"))
        .retrieve().bodyToMono(String.class).block();
        log.info("Result {}", body);
    }

}
