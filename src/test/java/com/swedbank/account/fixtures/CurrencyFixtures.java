package com.swedbank.account.fixtures;

import java.util.Currency;
import java.util.List;

public final class CurrencyFixtures {

	public static final Currency EUR = Currency.getInstance("EUR");
	public static final Currency USD = Currency.getInstance("USD");
	public static final Currency SEK = Currency.getInstance("SEK");
	public static final Currency GBP = Currency.getInstance("GBP");

	public static final Currency JPY = Currency.getInstance("JPY");

	public static final List<Currency> SUPPORTED_CURRENCIES = List.of(EUR, USD, SEK, GBP);

	public static final String INVALID_CURRENCY_CODE = "XYZ";

	private CurrencyFixtures() {
	}

}
