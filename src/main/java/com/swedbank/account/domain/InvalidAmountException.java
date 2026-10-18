package com.swedbank.account.domain;

public class InvalidAmountException extends IllegalArgumentException {

	public InvalidAmountException(String message) {
		super(message);
	}

}
