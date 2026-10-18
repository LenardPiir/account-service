package com.swedbank.account.service;

import com.swedbank.account.domain.Account;
import com.swedbank.account.persistence.AccountRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryAccountRepository implements AccountRepository {

	private final Map<UUID, Account> accounts = new HashMap<>();

	@Override
	public Optional<Account> findById(UUID id) {
		return Optional.ofNullable(accounts.get(id));
	}

	@Override
	public Optional<Account> findForUpdateById(UUID id) {
		return findById(id);
	}

	@Override
	public Account save(Account account) {
		accounts.put(account.getId(), account);
		return account;
	}

}
