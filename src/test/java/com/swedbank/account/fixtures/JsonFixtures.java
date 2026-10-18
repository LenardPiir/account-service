package com.swedbank.account.fixtures;

import java.util.Currency;
import java.util.UUID;

import static com.swedbank.account.fixtures.CurrencyFixtures.EUR;

public final class JsonFixtures {

	public static final String MALFORMED_JSON = "{\"currency\":\"EUR\",\"amount\":";

	private JsonFixtures() {
	}

	public static String createAccountJson(String ownerName) {
		if (ownerName == null) {
			return "{\"ownerName\":null}";
		}
		return "{\"ownerName\":\"%s\"}".formatted(ownerName);
	}

	public static String moneyJson(Currency currency, String amount) {
		return moneyJson(currency.getCurrencyCode(), amount);
	}

	public static String moneyJson(String currencyCode, String amount) {
		return "{\"currency\":\"%s\",\"amount\":%s}".formatted(currencyCode, amount);
	}

	public static String moneyJsonWithoutCurrency(String amount) {
		return "{\"amount\":%s}".formatted(amount);
	}

	public static String moneyJsonWithoutAmount(Currency currency) {
		return "{\"currency\":\"%s\"}".formatted(currency);
	}

	public static String aValidMoneyJson() {
		return moneyJson(EUR, "30.00");
	}

	public static String exchangeJson(Currency from, Currency to, String amount) {
		return "{\"from\":\"%s\",\"to\":\"%s\",\"amount\":%s}".formatted(from, to, amount);
	}

	public static String exchangeJsonWithoutFrom(Currency to, String amount) {
		return "{\"to\":\"%s\",\"amount\":%s}".formatted(to, amount);
	}

	public static String exchangeJsonWithoutTo(Currency from, String amount) {
		return "{\"from\":\"%s\",\"amount\":%s}".formatted(from, amount);
	}

	public static String accountJson(UUID id, String ownerName) {
		return "{\"id\":\"%s\",\"ownerName\":\"%s\"}".formatted(id, ownerName);
	}

	public static String balanceJson(Currency currency, String balance) {
		return "{\"currency\":\"%s\",\"balance\":%s}".formatted(currency, balance);
	}

	public static String balancesJson(UUID accountId, String... balanceJsons) {
		return "{\"accountId\":\"%s\",\"balances\":[%s]}".formatted(accountId, String.join(",", balanceJsons));
	}

	public static String exchangeResultJson(Currency from, Currency to, String debited, String credited) {
		return "{\"from\":\"%s\",\"to\":\"%s\",\"debited\":%s,\"credited\":%s}".formatted(from, to, debited, credited);
	}

	public static String problemJson(int status, String title, String detail) {
		return "{\"status\":%d,\"title\":\"%s\",\"detail\":\"%s\"}".formatted(status, title, detail);
	}

	public static String debitLogJson(UUID accountId, Currency currency, String amount) {
		return "{\"accountId\":\"%s\",\"currency\":\"%s\",\"amount\":%s}".formatted(accountId, currency, amount);
	}

}
