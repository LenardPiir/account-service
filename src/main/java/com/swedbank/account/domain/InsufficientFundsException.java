package com.swedbank.account.domain;

import java.util.Currency;

public class InsufficientFundsException extends RuntimeException {

	public InsufficientFundsException(Currency currency) {
		super("Insufficient funds in " + currency);
	}

}
