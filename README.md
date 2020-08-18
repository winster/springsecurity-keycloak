# Getting Started

### Sample application to demonstrate OAuth grant type client-credentials

#### CALLEE - A Resource Server
* `callee` has an endpoint which has to be secured - in OAuth2.0 terms, `callee` is a `Resource Server`
* `callee` will check for bearer token for all of its resources (by default all) - This is achieved by the libraries in classpath and `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`

#### CALLER - A Client
* `caller` has a background job calling the endpoint of callee - in OAuth2.0 terms, `caller` is a `Client` 
* Create a realm in Keycloak from `realm-export.json`. Regenerate secret from the credentials tab and copy the value to `spring.security.oauth2.client.registration.keycloak.client-secret` 
* Take a look at `WebclientConfig.java` for the `webclient` Bean and `authorizedClientManager` bean
* Now run both applications (using `mvn spring-boot:run` or from IDE or as you wish)

#### WEB - OIDC based browser flow
* A `web` module is added to show the grant type `authorization-code`. The code is based on https://spring.io/guides/tutorials/spring-boot-oauth2/ and extended to work with SSL enabled auth server.
* Create a realm in Keycloak from  `realm-export.json`. Confirm that browser-custom is selected as the browser flow in Authentication -> Bindings
* Confirm that `browser-custom` has `X509/Validate Username` execution and is `REQUIRED`. Read https://www.keycloak.org/docs/latest/server_admin/#_x509 for more info
* Install client certificate (in pfx format) in your browser. Read https://support.globalsign.com/digital-certificates/digital-certificate-installation/install-client-digital-certificate-windows-using-chrome for more info
* Run a reverse proxy (HAProxy used in this example) in front of Keycloak. Read docker-compose:services:hp and haproxy/conf & haproxy/cert folders
* Look at keycloak provider configuration to target reverse proxy domain/port instead of keycloak. In the example, /auth, /token & /userinfo are routed via RP. But /token & /userinfo can also directly point to Keycloak if RP configures certain http headers in /auth flow. (https://stackoverflow.com/questions/51554178/invalid-token-issuer-when-running-keycloak-behind-proxy)
* Look at https://gist.github.com/winster/5d41ebe94eabc3195f56091730f01092 in case you need help on certificate generation

### Learnings

#### Client Credentials flow
* `DefaultReactiveOAuth2AuthorizedClientManager` is not suitable for scheduled/background tasks as it is meant for httpservlet context and it will lead to `IllegalArgumentException: serverWebExchange cannot be null`.
So `AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager` should be used
* If `Client` is using `https` protocol with `Auth Server`, then `Resource Server` should also use `https` while validating the `token` against `Auth Server`. Otherwise `iss claim` check will fail.
* But previous statement is not valid for authorization_code grant type. I have used https uri for all endpoints except jwkset and it worked. `jwkset` was excluded as I could not find a way to use a custom JwtDecoder. See it working in `web` module 
* Spring Oauth Jose library 5.3.3 supports both JwtDecoder and ReactiveJwtDecoder. If you want to use webclient use the latter
* With access token lifespan at 1 minute, causes the Client to request for new token every time. Client's scheduled job runs every 10 seconds. Changing the lifespan to 5 minutes seems to resolve the issue. OTOH, explicit logout of sessions from Auth Server does not seem to have any effect on Client or Resource Server. Yet to explore further...

#### Browser flow-x509
* At least to me, Keycloak documentation was not sufficient to start. Learning is that when Keycloak says add some execution in Browser flow, it means that the user agent first visits your web application and then redirects to keycloak for auth. And Keycloak can be behind a reverse proxy
* If you want to enable client certificate based authentication based on http path (say customer X wants username/password login form whereas customer Y wants x509 certificate based auth), it is not possible (please correct me if I'm wrong) to do so in HAProxy.
* Apache httpd could be a good candidate (https://httpd.apache.org/docs/current/mod/core.html#locationmatch and https://httpd.apache.org/docs/current/mod/mod_ssl.html#SSLVerifyClient)


### References

* https://medium.com/@karanbir.tech/spring-boot-oauth2-mutual-tls-client-client-credentials-grant-3cdb7a2a44ea
* https://www.haproxy.org/download/1.5/doc/configuration.txt
* Email support from Peter Nalyvayko <brat000012001@gmail.com> in keycloak-user google group forum