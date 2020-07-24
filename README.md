# Getting Started

### Sample application to demonstrate OAuth grant type client-credentials

* callee has an endpoint which has to be secured - in OAuth2.0 terms, calle is Resource Server
* calle will check for bearer token for all of its resources (by default all) - This is achieved by the libraries in classpath and spring.security.oauth2.resourceserver.jwt.jwk-set-uri
* caller has a background job calling the endpoint of callee - in OAuth2.0 terms, caller is Client 
* In Keycloak create a realm from 'realm-export.json'. Regenerate secret from the credentials tab and copy the value to spring.security.oauth2.client.registration.keycloak.client-secret 
* Take a look at WebclientConfig.java for the webclient Bean and authorizedClientManager bean
* Now run both applications (using mvn spring-boot:run or from IDE or as you wish)


### Known Problems

* If token URL is https, there will be SSLHandshakeException - Yet to explore
* A token is generated everytime before the last one expires - Yet to explore
