package com.example.caller.config;


import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;


/**
 * Class WebclientConfig
 */
@EnableWebFluxSecurity
@Slf4j
public class WebclientConfig
{

    @Value("${oauth2.client.registration.keycloak.ssl-enabled}")
    private boolean sslEnabled;
    @Value("${oauth2.client.registration.keycloak.truststore}")
    private Resource trustStore;
    @Value("${oauth2.client.registration.keycloak.truststore-password}")
    private String trustStorePassword;

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
     * The Oauth2 based WebClient bean for the controller service
     */
    @Bean
    public WebClient.Builder webClientBuilder(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService auth2AuthorizedClientService)
        throws SSLException, NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, KeyManagementException
    {
        String registrationId = "keycloak";

        if (sslEnabled) {
            /*TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(
                            TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(
                    trustStore.getInputStream(),
                    trustStorePassword.toCharArray());
            trustManagerFactory.init(keyStore);*/

            SslContext sslContext = SslContextBuilder.forClient()
                    //.clientAuth(ClientAuth.REQUIRE)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            WebClientReactiveClientCredentialsTokenResponseClient accessTokenResponseClient = new WebClientReactiveClientCredentialsTokenResponseClient();

            // create httpClient based on customized SSLContext
            // where SSLContext is constructed based on the properties in the properties file
            // refer OAuth2ClientSSLPropertiesConfigurer for that
            HttpClient httpClient = HttpClient.create()
                    .secure(t -> {
                        // retrieve the sslContext from the oAuth2ClientSSLPropertiesConfigurer
                        // based on the registrationId
                        t.sslContext(sslContext);
                    });
            ClientHttpConnector httpConnector = new ReactorClientHttpConnector(httpClient);

            WebClient webClient = WebClient.builder().clientConnector(httpConnector).build();

            accessTokenResponseClient.setWebClient(webClient);

            // create custom authorizedClientProvider based on custom accessTokenResponseClient
            ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder
                    .builder()
                    .clientCredentials(c -> {
                        c.accessTokenResponseClient(accessTokenResponseClient);
                    }).build();

            // override default authorizedClientManager based on custom authorizedClientProvider
            authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, auth2AuthorizedClientService);

            ((AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager) authorizedClientManager).setAuthorizedClientProvider(authorizedClientProvider);
        }

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
                        .filter(logResponse());
    }


    /*
     * Log request details for the downstream controller service calls
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
     * Log response details for the downstream controller service calls
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
