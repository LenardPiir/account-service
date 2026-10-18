package com.swedbank.account.api.dto;

import java.math.BigDecimal;
import java.util.Currency;

public record ExchangeResponse(Currency from, Currency to, BigDecimal debited, BigDecimal credited) {

	public ExchangeResponse {
		debited = Amounts.inCurrencyScale(from, debited);
		credited = Amounts.inCurrencyScale(to, credited);
	}

}
