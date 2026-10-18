package com.swedbank.account.fixtures;

import java.util.UUID;

public final class IdempotencyFixtures {

	public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

	public static final String NO_KEY = null;

	public static final int MAX_KEY_LENGTH = 64;

	private IdempotencyFixtures() {
	}

	public static String aKey() {
		return UUID.randomUUID().toString();
	}

	public static String aKeyTooLong() {
		return "k".repeat(MAX_KEY_LENGTH + 1);
	}

}
