package com.swedbank.account.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "external-logging")
public record ExternalLoggingProperties(URI url, Duration connectTimeout, Duration readTimeout) {
}
