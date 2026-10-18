package com.swedbank.account.fixtures;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static com.swedbank.account.fixtures.EndpointFixtures.ACCOUNTS_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.ACCOUNT_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.BALANCES_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.CREDITS_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.DEBITS_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.EXCHANGES_PATH;
import static com.swedbank.account.fixtures.IdempotencyFixtures.IDEMPOTENCY_KEY_HEADER;
import static com.swedbank.account.fixtures.IdempotencyFixtures.NO_KEY;

public final class AccountApiClient {

	private final MockMvcTester mvc;

	public AccountApiClient(MockMvcTester mvc) {
		this.mvc = mvc;
	}

	public MvcTestResult createAccount(String json) {
		return mvc.post().uri(ACCOUNTS_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json)
				.exchange();
	}

	public MvcTestResult getAccount(UUID accountId) {
		return mvc.get().uri(ACCOUNT_PATH, accountId).exchange();
	}

	public MvcTestResult balances(UUID accountId) {
		return mvc.get().uri(BALANCES_PATH, accountId).exchange();
	}

	public MvcTestResult credit(UUID accountId, String json) {
		return post(CREDITS_PATH, accountId, json, NO_KEY);
	}

	public MvcTestResult debit(UUID accountId, String json) {
		return debit(accountId, json, NO_KEY);
	}

	public MvcTestResult debit(UUID accountId, String json, String idempotencyKey) {
		return post(DEBITS_PATH, accountId, json, idempotencyKey);
	}

	public MvcTestResult exchange(UUID accountId, String json) {
		return post(EXCHANGES_PATH, accountId, json, NO_KEY);
	}

	private MvcTestResult post(String pathTemplate, UUID accountId, String json, String idempotencyKey) {
		MockMvcTester.MockMvcRequestBuilder request = mvc.post().uri(pathTemplate, accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
		if (idempotencyKey != null) {
			request.header(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
		}
		return request.exchange();
	}

}
