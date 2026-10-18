package com.swedbank.account.exchange;

import com.swedbank.account.domain.CurrencyConverter;
import com.swedbank.account.domain.UnsupportedCurrencyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Map;
import java.util.Set;

public class ExchangeRates implements CurrencyConverter {

	private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

	private final Map<Currency, BigDecimal> ratesFromEur;

	public ExchangeRates(Map<Currency, BigDecimal> ratesFromEur) {
		this.ratesFromEur = Map.copyOf(ratesFromEur);
		this.ratesFromEur.forEach(ExchangeRates::requirePositive);
	}

	public boolean supports(Currency currency) {
		return ratesFromEur.containsKey(currency);
	}

	public Set<Currency> supportedCurrencies() {
		return ratesFromEur.keySet();
	}

	@Override
	public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
		requireSupported(from);
		requireSupported(to);
		if (from.equals(to)) {
			return amount;
		}
		return amount.multiply(ratesFromEur.get(to))
				.divide(ratesFromEur.get(from), to.getDefaultFractionDigits(), ROUNDING);
	}

	private void requireSupported(Currency currency) {
		if (!supports(currency)) {
			throw new UnsupportedCurrencyException(currency);
		}
	}

	private static void requirePositive(Currency currency, BigDecimal rate) {
		if (rate.signum() <= 0) {
			throw new IllegalArgumentException("Exchange rate for " + currency + " must be positive");
		}
	}

}
