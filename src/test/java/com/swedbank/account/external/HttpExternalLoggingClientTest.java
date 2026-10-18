package com.swedbank.account.external;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.swedbank.account.fixtures.AmountFixtures.amount;
import static com.swedbank.account.fixtures.CurrencyFixtures.EUR;
import static com.swedbank.account.fixtures.EndpointFixtures.EXTERNAL_LOG_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.UNREACHABLE_URL;
import static com.swedbank.account.fixtures.JsonFixtures.debitLogJson;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpExternalLoggingClientTest {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);
	private static final Duration READ_TIMEOUT = Duration.ofMillis(500);

	@RegisterExtension
	static WireMockExtension externalSystem = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	private final UUID accountId = UUID.randomUUID();

	private HttpExternalLoggingClient clientFor(String url) {
		return new HttpExternalLoggingClient(RestClient.builder(),
				new ExternalLoggingProperties(URI.create(url), CONNECT_TIMEOUT, READ_TIMEOUT));
	}

	private HttpExternalLoggingClient client() {
		return clientFor(externalSystem.baseUrl() + EXTERNAL_LOG_PATH);
	}

	@Test
	void postsDebitDetailsToExternalSystem() {
		externalSystem.stubFor(post(EXTERNAL_LOG_PATH).willReturn(ok()));

		client().logDebit(accountId, EUR, amount("30.00"));

		externalSystem.verify(postRequestedFor(urlEqualTo(EXTERNAL_LOG_PATH))
				.withRequestBody(equalToJson(debitLogJson(accountId, EUR, "30.00"))));
	}

	@Test
	void serverErrorIsReportedAsExternalSystemFailure() {
		externalSystem.stubFor(post(EXTERNAL_LOG_PATH).willReturn(serverError()));

		assertThatThrownBy(() -> client().logDebit(accountId, EUR, amount("30.00")))
				.isInstanceOf(ExternalSystemException.class);
	}

	@Test
	void slowResponseIsReportedAsExternalSystemFailure() {
		externalSystem.stubFor(post(EXTERNAL_LOG_PATH).willReturn(aResponse()
				.withStatus(200)
				.withFixedDelay((int) READ_TIMEOUT.multipliedBy(4).toMillis())));

		assertThatThrownBy(() -> client().logDebit(accountId, EUR, amount("30.00")))
				.isInstanceOf(ExternalSystemException.class);
	}

	@Test
	void unreachableSystemIsReportedAsExternalSystemFailure() {
		HttpExternalLoggingClient client = clientFor(UNREACHABLE_URL);

		assertThatThrownBy(() -> client.logDebit(accountId, EUR, amount("30.00")))
				.isInstanceOf(ExternalSystemException.class);
	}

}
