package com.swedbank.account.domain;

import java.util.Currency;

public class UnsupportedCurrencyException extends RuntimeException {

	public UnsupportedCurrencyException(Currency currency) {
		super("Currency not supported: " + currency);
	}

}
