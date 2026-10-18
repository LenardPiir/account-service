package com.swedbank.account.external;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExternalLoggingProperties.class)
public class ExternalLoggingConfiguration {

	@Bean
	ExternalLoggingClient externalLoggingClient(RestClient.Builder builder, ExternalLoggingProperties properties) {
		return new HttpExternalLoggingClient(builder, properties);
	}

}
