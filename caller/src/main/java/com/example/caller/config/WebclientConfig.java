package com.example.caller.config;


import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import reactor.netty.http.client.HttpClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;


/**
 * Class WebclientConfig
 */
@EnableWebFluxSecurity
@Slf4j
public class WebclientConfig
{

    /**
     * The authorizedClientManager for required by the webClient
     * This bean throws an error, IllegalArgumentException: serverWebExchange cannot be null
     */

    /*
     * @Bean
     * public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(final ReactiveClientRegistrationRepository clientRegistrationRepository,
     *                                                                    final ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
     *   ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
     *           .clientCredentials()
     *           .build();
     *
     *   DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
     *
     *   authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
     *   return authorizedClientManager;
     * }
     */


    /**
     * The authorizedClientManager for required by the webClient
     */
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService)
    {
        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
            ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                                                         .clientCredentials()
                                                         .build();

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
            new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(
            authorizedClientProvider);

        return authorizedClientManager;
    }

    /**
     * The Oauth2 based WebClient bean for the web service
     */
    @Bean
    public WebClient webClient(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager)
        throws SSLException
    {
        String registrationId = "keycloak";

        SslContext sslContext = SslContextBuilder.forClient()
                                                 .trustManager(
                                                     InsecureTrustManagerFactory.INSTANCE)
                                                 .build();
        HttpClient httpClient = HttpClient.create()
                                          .secure(
                                              t -> t.sslContext(sslContext));

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
            new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                authorizedClientManager);

        // for telling which registration to use for the webclient
        oauth.setDefaultClientRegistrationId(registrationId);

        return WebClient.builder()
                        .clientConnector(
                            new ReactorClientHttpConnector(httpClient))

        // base path of the client, this way we need to set the complete url again
        .filter(oauth)
                        .filter(logRequest())
                        .filter(logResponse())
                        .build();
    }

    /*
     * Log request details for the downstream web service calls
     */
    private ExchangeFilterFunction logRequest()
    {
        return ExchangeFilterFunction.ofRequestProcessor(
            c -> {
                log.info("Request: {} {}", c.method(), c.url());
                c.headers().forEach((n, v) -> {
                        if (!n.equalsIgnoreCase(AUTHORIZATION)) {
                            log.info("request header {}={}", n, v);
                        } else {
                            // as the AUTHORIZATION header is something security bounded
                            // will show up when the debug level logging is enabled
                            // for example using property - logging.level.root=DEBUG
                            log.debug("request header {}={}", n, v);
                        }
                    });

                return Mono.just(c);
            });
    }

    /*
     * Log response details for the downstream web service calls
     */
    private ExchangeFilterFunction logResponse()
    {
        return ExchangeFilterFunction.ofResponseProcessor(
            c -> {
                log.info("Response: {} {}", c.statusCode());

                // if want to show the response headers in the log by any chance?

                /*
                 * c.headers().asHttpHeaders().forEach((n, v) -> {
                 *   testWebClientLogger.info("response header {}={}", n, v);
                 * });
                 */
                return Mono.just(c);
            });
    }

    /*
     * Even without this bean, the application works - To be researched
     *
     * @Bean
     * public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
     *   return http.oauth2Client().and().build();
     * }
     */
}
