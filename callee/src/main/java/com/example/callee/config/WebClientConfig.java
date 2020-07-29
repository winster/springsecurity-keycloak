package com.example.callee.config;


import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;


@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${ssl.keycloak.enabled}")
    private boolean keycloakSslEnabled;
    @Value("${ssl.keycloak.truststore}")
    private Resource keycloakTrustStore;
    @Value("${ssl.keycloak.truststore-password}")
    private String keycloakTrustStorePassword;


    @Bean
    public WebClient.Builder authServerWebClientBuilder()
            throws IOException, NoSuchAlgorithmException, KeyStoreException,
            CertificateException {
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
