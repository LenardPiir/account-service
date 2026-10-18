package com.swedbank.account.api.dto;

import java.math.BigDecimal;
import java.util.Currency;

public record BalanceResponse(Currency currency, BigDecimal balance) {

	public BalanceResponse {
		balance = Amounts.inCurrencyScale(currency, balance);
	}

}
