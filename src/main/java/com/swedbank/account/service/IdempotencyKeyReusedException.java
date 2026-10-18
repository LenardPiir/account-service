package com.swedbank.account.service;

public class IdempotencyKeyReusedException extends RuntimeException {

	public IdempotencyKeyReusedException(String idempotencyKey) {
		super("Idempotency key " + idempotencyKey + " was already used for a different request");
	}

}
