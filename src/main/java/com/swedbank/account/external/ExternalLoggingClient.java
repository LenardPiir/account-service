package com.swedbank.account.external;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

public interface ExternalLoggingClient {

	void logDebit(UUID accountId, Currency currency, BigDecimal amount);

}
