package com.romif.securityalarm.androidclient;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;

import java.util.Arrays;

public class ResourceConfiguration {

    private static OAuth2ProtectedResourceDetails resource(String accessTokenUri, String clientID, String clientSecret, String username, String password) {
        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
        resource.setAccessTokenUri(accessTokenUri);
        resource.setClientId(clientID);
        resource.setClientSecret(clientSecret);
        resource.setGrantType("password");
        resource.setScope(Arrays.asList("write", "read"));
        resource.setUsername(username);
        resource.setPassword(password);

        return resource;
    }

    public static OAuth2RestTemplate restTemplate(String accessTokenUri, String clientID, String clientSecret, String username, String password) {
        AccessTokenRequest atr = new DefaultAccessTokenRequest();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setOutputStreaming(false);
        requestFactory.setReadTimeout(60 * 1000);
        requestFactory.setConnectTimeout(60 * 1000);
        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resource(accessTokenUri, clientID, clientSecret, username, password), new DefaultOAuth2ClientContext(atr));
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }
}
