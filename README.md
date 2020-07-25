# Getting Started

### Sample application to demonstrate OAuth grant type client-credentials

* `callee` has an endpoint which has to be secured - in OAuth2.0 terms, `callee` is a `Resource Server`
* `callee` will check for bearer token for all of its resources (by default all) - This is achieved by the libraries in classpath and `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`
* `caller` has a background job calling the endpoint of callee - in OAuth2.0 terms, `caller` is a `Client` 
* In Keycloak create a realm from `realm-export.json`. Regenerate secret from the credentials tab and copy the value to `spring.security.oauth2.client.registration.keycloak.client-secret` 
* Take a look at `WebclientConfig.java` for the `webclient` Bean and `authorizedClientManager` bean
* Now run both applications (using `mvn spring-boot:run` or from IDE or as you wish)


### Known Problems

* A token is generated everytime before the last one expires - Yet to explore


### Learnings

* `DefaultReactiveOAuth2AuthorizedClientManager` is not suitable for scheduled/background tasks as it is meant for httpservlet context and it will lead to `IllegalArgumentException: serverWebExchange cannot be null`.
So `AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager` should be used

* If `Client` is using `https` protocol with `Auth Server`, then `Resource Server` should also use `https` while validating the `token` against `Auth Server`. Otherwise `iss claim` check will fail

* Spring Oauth Jose library still uses RestTemplate