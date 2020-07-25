# Getting Started

### Sample application to demonstrate OAuth grant type client-credentials

* Create a service (callee) - This is a Resource Server in OAuth2.0 terms
* Make the resource server to check for bearer token for all the resources (by default all)
* Create an application (caller) which has a background job calling the service (Callee) - This is a Client in OAuth2.0 terms 
* In Keycloak create a realm from 'realm-export.json'. Regenerate secret from the credentials tab and copy the value to spring.security.oauth2.client.registration.keycloak.client-secret 
* Take a look at WebclientConfig.java for the webclient Bean and authorizedClientManager bean


### Known Problems

* If token URL is https, there will be SSLHandshakeException - Yet to explore
* A token is generated everytime before the last one expires - Yet to explore


### Learnings

* `DefaultReactiveOAuth2AuthorizedClientManager` is not suitable for scheduled/background tasks as it is meant for httpservlet context and it will lead to `IllegalArgumentException: serverWebExchange cannot be null`.
So `AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager` should be used
