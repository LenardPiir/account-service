package com.swedbank.account.domain;

import java.math.BigDecimal;
import java.util.Currency;

public interface CurrencyConverter {

	BigDecimal convert(BigDecimal amount, Currency from, Currency to);

}
