package com.swedbank.account.fixtures;

import com.swedbank.account.domain.Account;

import java.util.Currency;

import static com.swedbank.account.fixtures.AmountFixtures.amount;

public final class AccountFixtures {

	public static final String OWNER_NAME = "Test Owner";

	private AccountFixtures() {
	}

	public static Account anAccount() {
		return new Account(OWNER_NAME);
	}

	public static Account anAccountWith(Currency currency, String balance) {
		Account account = anAccount();
		account.credit(currency, amount(balance));
		return account;
	}

}
