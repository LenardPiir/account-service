package com.swedbank.account.external;

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;
import java.util.UUID;

public class HttpExternalLoggingClient implements ExternalLoggingClient {

	private final RestClient restClient;
	private final URI url;

	public HttpExternalLoggingClient(RestClient.Builder builder, ExternalLoggingProperties properties) {
		this.restClient = builder.requestFactory(requestFactoryWithTimeouts(properties)).build();
		this.url = properties.url();
	}

	@Override
	public void logDebit(UUID accountId, Currency currency, BigDecimal amount) {
		try {
			restClient.post()
					.uri(url)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new DebitLogEntry(accountId, currency, amount))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException e) {
			throw new ExternalSystemException("External logging failed: " + e.getMessage(), e);
		}
	}

	private static ClientHttpRequestFactory requestFactoryWithTimeouts(ExternalLoggingProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());
		return requestFactory;
	}

	record DebitLogEntry(UUID accountId, Currency currency, BigDecimal amount) {
	}

}
