package com.swedbank.account.fixtures;

import java.util.UUID;

public final class EndpointFixtures {

	public static final String ACCOUNTS_PATH = "/api/v1/accounts";
	public static final String ACCOUNT_PATH = ACCOUNTS_PATH + "/{id}";
	public static final String BALANCES_PATH = ACCOUNT_PATH + "/balances";
	public static final String CREDITS_PATH = ACCOUNT_PATH + "/credits";
	public static final String DEBITS_PATH = ACCOUNT_PATH + "/debits";
	public static final String EXCHANGES_PATH = ACCOUNT_PATH + "/exchanges";
	public static final String API_DOCS_PATH = "/v3/api-docs";
	public static final String SWAGGER_UI_PATH = "/swagger-ui/index.html";

	public static final String MOCK_MVC_BASE_URL = "http://localhost";

	public static final String EXTERNAL_LOG_PATH = "/log";

	public static final String UNREACHABLE_URL = "http://localhost:1" + EXTERNAL_LOG_PATH;

	private EndpointFixtures() {
	}

	public static String accountUrl(UUID accountId) {
		return MOCK_MVC_BASE_URL + ACCOUNTS_PATH + "/" + accountId;
	}

}
