package com.example.caller.config;


import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import lombok.var;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import reactor.netty.tcp.TcpClient;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
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

    String registrationId="keycloak";

    @Value("${ssl.keycloak.enabled}")
    private boolean keycloakSslEnabled;
    @Value("${ssl.keycloak.truststore}")
    private Resource keycloakTrustStore;
    @Value("${ssl.keycloak.truststore-password}")
    private String keycloakTrustStorePassword;

    @Value("${ssl.callee.enabled}")
    private boolean calleeSslEnabled;
    @Value("${ssl.callee.truststore}")
    private Resource calleeTrustStore;
    @Value("${ssl.callee.truststore-password}")
    private String calleeTrustStorePassword;

    @Bean
    public WebClient.Builder authServerWebClientBuilder()
            throws IOException, NoSuchAlgorithmException, KeyStoreException,
            CertificateException
    {
        SslContext sslContext = createSslContext(keycloakSslEnabled,
                keycloakTrustStore,
                keycloakTrustStorePassword);

        HttpClient httpClient = HttpClient.create()
                .secure(
                        t -> t.sslContext(sslContext));
        ClientHttpConnector httpConnector =
                new ReactorClientHttpConnector(httpClient);

        return WebClient.builder().clientConnector(httpConnector);
    }

    /**
     * The Oauth2 based WebClient bean for the scheduler service. This custom builder will introduce an exchange filter
     * to oauth the request to Resource Server. Most of the code in the below method is about defining the oauth function
     * and securing it (https://keycloak)
     */
    @Bean
    public WebClient.Builder webClientBuilder(
            WebClient.Builder authServerWebClientBuilder,
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService auth2AuthorizedClientService)
    {
        WebClientReactiveClientCredentialsTokenResponseClient accessTokenResponseClient =
                new WebClientReactiveClientCredentialsTokenResponseClient();
        accessTokenResponseClient.setWebClient(
                authServerWebClientBuilder.build());

        // create custom authorizedClientProvider based on custom accessTokenResponseClient
        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials(
                                c -> c.accessTokenResponseClient(
                                        accessTokenResponseClient))
                        .build();

        // override default authorizedClientManager based on custom authorizedClientProvider
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, auth2AuthorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(
                authorizedClientProvider);

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        authorizedClientManager);

        // for telling which registration to use for the webclient
        oauth.setDefaultClientRegistrationId(registrationId);

        return WebClient.builder()
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
                    log.info("Response: {}", c.statusCode());

                    // if want to show the response headers in the log by any chance?

                    /*
                     * c.headers().asHttpHeaders().forEach((n, v) -> {
                     *   testWebClientLogger.info("response header {}={}", n, v);
                     * });
                     */
                    return Mono.just(c);
                });
    }

    @SneakyThrows
    @Bean(name = "calleeWebClientBuilder")
    public WebClient.Builder calleeWebClientBuilder(WebClient.Builder webClientBuilder)
    {
        log.debug("calleeWebClient :: new webclient");

        SslContext sslContext = createSslContext(calleeSslEnabled,
                calleeTrustStore,
                calleeTrustStorePassword);
        var tcpClient = TcpClient.create()
                .secure(t -> t.sslContext(sslContext));

        return webClientBuilder
                .clientConnector(
                        new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
                .defaultHeader(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    private SslContext createSslContext(boolean sslEnabled,
                                        Resource trustStore,
                                        String trustStorePassword)
            throws IOException, NoSuchAlgorithmException, KeyStoreException,
            CertificateException {
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        if (sslEnabled) {
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(
                            TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(trustStore.getInputStream(),
                    trustStorePassword.toCharArray());
            trustManagerFactory.init(keyStore);
            sslContextBuilder.trustManager(trustManagerFactory);
        } else {
            sslContextBuilder.trustManager(
                    InsecureTrustManagerFactory.INSTANCE);
        }

        return sslContextBuilder.build();
    }
}
