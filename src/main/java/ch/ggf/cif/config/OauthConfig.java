package ch.ggf.cif.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;

@Configuration
@EnableAspectJAutoProxy
public class OauthConfig {

  @Value("${config.oauth2.accessTokenUri}")
  private String accessTokenUri;

  @Value("${config.oauth2.clientID}")
  private String clientId;

  @Value("${config.oauth2.clientSecret}")
  private String clientSecret;

	public OAuth2RestOperations oauthRestOperations() {
		return new OAuth2RestTemplate(resource(), singletonClientContext());
	}

	private OAuth2ProtectedResourceDetails resource() {
		ClientCredentialsResourceDetails resource  = new ClientCredentialsResourceDetails();
		resource.setClientId(clientId);
		resource.setAccessTokenUri(accessTokenUri);
		resource.setScope(Arrays.asList("read"));
		resource.setClientSecret(clientSecret);
		return resource;
	}

	@Primary
	@Bean
	public OAuth2ClientContext singletonClientContext() {
	    return new DefaultOAuth2ClientContext();
	}

}
