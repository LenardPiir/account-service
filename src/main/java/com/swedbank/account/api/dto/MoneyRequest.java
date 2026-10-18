package com.swedbank.account.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;

public record MoneyRequest(
		@NotNull(message = "currency is required") Currency currency,
		@NotNull(message = "amount is required") @Positive(message = "amount must be greater than 0") BigDecimal amount) {
}
