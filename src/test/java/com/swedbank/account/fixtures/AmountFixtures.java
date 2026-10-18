package com.swedbank.account.fixtures;

import java.math.BigDecimal;

public final class AmountFixtures {

	private AmountFixtures() {
	}

	public static BigDecimal amount(String value) {
		return new BigDecimal(value);
	}

}
