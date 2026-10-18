package com.swedbank.account.api.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

final class Amounts {

	private Amounts() {
	}

	static BigDecimal inCurrencyScale(Currency currency, BigDecimal amount) {
		return amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.UNNECESSARY);
	}

}
