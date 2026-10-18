package com.swedbank.account.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;

public record ExchangeRequest(
		@NotNull(message = "from is required") Currency from,
		@NotNull(message = "to is required") Currency to,
		@NotNull(message = "amount is required") @Positive(message = "amount must be greater than 0") BigDecimal amount) {

	@JsonIgnore
	@SuppressWarnings("unused")
	@AssertTrue(message = "from and to must be different currencies")
	public boolean isDifferentCurrencies() {
		return from == null || !from.equals(to);
	}

}
