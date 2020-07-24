# Getting Started

### Sample application to demonstrate OAuth grant type client-credentials

* Create a service (callee) - This is a Resource Server in OAuth2.0 terms
* Make the resource server to check for bearer token for all the resources (by default all)
* Create an application (caller) which has a background job calling the service (Callee) - This is a Client in OAuth2.0 terms 
* In Keycloak create a client for caller in your Realm. Enable Service Account in settings
* Take a look at WebclientConfig.java for the webclient Bean and authorizedClientManager bean


### Known Problems

* If token URL is https, there will be SSLHandshakeException - Yet to explore
* A token is generated everytime before the last one expires - Yet to explore