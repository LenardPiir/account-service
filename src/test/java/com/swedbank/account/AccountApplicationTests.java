package com.swedbank.account;

import com.swedbank.account.api.dto.ExchangeRequest;
import com.swedbank.account.api.dto.MoneyRequest;
import com.swedbank.account.exchange.ExchangeRates;
import com.swedbank.account.fixtures.ApiDocsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Stream;

import static com.swedbank.account.fixtures.CurrencyFixtures.SUPPORTED_CURRENCIES;
import static com.swedbank.account.fixtures.EndpointFixtures.ACCOUNTS_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.ACCOUNT_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.BALANCES_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.CREDITS_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.DEBITS_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.EXCHANGES_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@Import({TestcontainersConfiguration.class, ApiDocsClient.class})
@SpringBootTest
@AutoConfigureMockMvc
class AccountApplicationTests {

	@Autowired
	private ExchangeRates exchangeRates;

	@Autowired
	private ApiDocsClient apiDocs;

	@Test
	void contextLoads() {
	}

	@Test
	void supportsTheFourCurrenciesFromTheAssignment() {
		assertThat(exchangeRates.supportedCurrencies()).containsExactlyInAnyOrderElementsOf(SUPPORTED_CURRENCIES);
	}

	@Test
	void swaggerUiIsServed() {
		assertThat(apiDocs.swaggerUi()).hasStatusOk();
	}

	@Test
	void apiDocumentationDescribesCurrencyAsIsoCode() {
		assertThat(apiDocs.documentedType(MoneyRequest.class, "currency")).isEqualTo("string");
	}

	@Test
	void apiDocumentationShowsOnlyRealExchangeRequestFields() {
		assertThat(apiDocs.documentedFields(ExchangeRequest.class)).containsOnly("from", "to", "amount");
	}

	static Stream<Arguments> documentedResponses() {
		return Stream.of(
				arguments(POST, ACCOUNTS_PATH, List.of(CREATED, BAD_REQUEST)),
				arguments(GET, ACCOUNT_PATH, List.of(OK, BAD_REQUEST, NOT_FOUND)),
				arguments(GET, BALANCES_PATH, List.of(OK, BAD_REQUEST, NOT_FOUND)),
				arguments(POST, CREDITS_PATH, List.of(OK, BAD_REQUEST, NOT_FOUND, CONFLICT, UNPROCESSABLE_CONTENT)),
				arguments(POST, DEBITS_PATH,
						List.of(OK, BAD_REQUEST, NOT_FOUND, CONFLICT, UNPROCESSABLE_CONTENT, SERVICE_UNAVAILABLE)),
				arguments(POST, EXCHANGES_PATH, List.of(OK, BAD_REQUEST, NOT_FOUND, CONFLICT, UNPROCESSABLE_CONTENT)));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("documentedResponses")
	void apiDocumentationListsTheResponsesEachEndpointCanReturn(HttpMethod method, String path,
			List<HttpStatus> statuses) {
		assertThat(apiDocs.documentedStatuses(method, path)).containsExactlyInAnyOrderElementsOf(statuses);
	}

}
