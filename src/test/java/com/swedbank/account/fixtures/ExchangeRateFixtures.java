package com.swedbank.account.fixtures;

import com.swedbank.account.exchange.ExchangeRates;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import static com.swedbank.account.fixtures.AmountFixtures.amount;
import static com.swedbank.account.fixtures.CurrencyFixtures.EUR;
import static com.swedbank.account.fixtures.CurrencyFixtures.GBP;
import static com.swedbank.account.fixtures.CurrencyFixtures.SEK;
import static com.swedbank.account.fixtures.CurrencyFixtures.USD;

public final class ExchangeRateFixtures {

	public static final String EUR_RATE = "1";
	public static final String USD_RATE = "1.10";
	public static final String SEK_RATE = "11.00";
	public static final String GBP_RATE = "0.85";

	private ExchangeRateFixtures() {
	}

	public static Map<Currency, BigDecimal> testRates() {
		Map<Currency, BigDecimal> rates = new HashMap<>();
		rates.put(EUR, amount(EUR_RATE));
		rates.put(USD, amount(USD_RATE));
		rates.put(SEK, amount(SEK_RATE));
		rates.put(GBP, amount(GBP_RATE));
		return rates;
	}

	public static ExchangeRates testExchangeRates() {
		return new ExchangeRates(testRates());
	}

}
