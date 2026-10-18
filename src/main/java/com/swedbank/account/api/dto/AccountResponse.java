package com.swedbank.account.api.dto;

import com.swedbank.account.domain.Account;

import java.util.UUID;

public record AccountResponse(UUID id, String ownerName) {

	public static AccountResponse from(Account account) {
		return new AccountResponse(account.getId(), account.getOwnerName());
	}

}
