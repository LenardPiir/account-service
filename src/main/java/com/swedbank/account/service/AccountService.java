package com.swedbank.account.service;

import com.swedbank.account.domain.Account;
import com.swedbank.account.domain.AccountNotFoundException;
import com.swedbank.account.domain.UnsupportedCurrencyException;
import com.swedbank.account.exchange.ExchangeRates;
import com.swedbank.account.external.ExternalLoggingClient;
import com.swedbank.account.persistence.AccountRepository;
import com.swedbank.account.persistence.ProcessedRequest;
import com.swedbank.account.persistence.ProcessedRequestRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Currency;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final AccountRepository accounts;
	private final ExchangeRates exchangeRates;
	private final ExternalLoggingClient externalLogging;
	private final ProcessedRequestRepository processedRequests;

	@Transactional
	public Account createAccount(String ownerName) {
		return accounts.save(new Account(ownerName));
	}

	@Transactional(readOnly = true)
	public Account getAccount(UUID accountId) {
		return findAccount(accountId);
	}

	@Transactional(readOnly = true)
	public Map<Currency, BigDecimal> balances(UUID accountId) {
		Account account = findAccount(accountId);
		Map<Currency, BigDecimal> balances = new TreeMap<>(Comparator.comparing(Currency::getCurrencyCode));
		for (Currency currency : exchangeRates.supportedCurrencies()) {
			balances.put(currency, account.balanceOf(currency));
		}
		return balances;
	}

	@Transactional
	public BigDecimal credit(UUID accountId, Currency currency, BigDecimal amount, @Nullable String idempotencyKey) {
		requireSupported(currency);
		Account account = findAccountForUpdate(accountId);
		return onlyOnce(accountId, idempotencyKey, fingerprint(Operation.CREDIT, currency, amount), () -> {
			account.credit(currency, amount);
			accounts.save(account);
			return account.balanceOf(currency);
		});
	}

	@Transactional
	public BigDecimal debit(UUID accountId, Currency currency, BigDecimal amount, @Nullable String idempotencyKey) {
		requireSupported(currency);
		Account account = findAccountForUpdate(accountId);
		return onlyOnce(accountId, idempotencyKey, fingerprint(Operation.DEBIT, currency, amount), () -> {
			account.requireCanDebit(currency, amount);
			externalLogging.logDebit(accountId, currency, amount);
			account.debit(currency, amount);
			accounts.save(account);
			return account.balanceOf(currency);
		});
	}

	@Transactional
	public BigDecimal exchange(UUID accountId, Currency from, Currency to, BigDecimal amount,
			@Nullable String idempotencyKey) {
		requireSupported(from);
		requireSupported(to);
		Account account = findAccountForUpdate(accountId);
		return onlyOnce(accountId, idempotencyKey, fingerprint(Operation.EXCHANGE, from, to, amount), () -> {
			BigDecimal credited = account.exchange(from, to, amount, exchangeRates);
			accounts.save(account);
			return credited;
		});
	}

	private BigDecimal onlyOnce(UUID accountId, @Nullable String idempotencyKey, String fingerprint,
			Supplier<BigDecimal> operation) {
		if (idempotencyKey == null) {
			return operation.get();
		}
		Optional<ProcessedRequest> previous = processedRequests.findByAccountIdAndIdempotencyKey(accountId,
				idempotencyKey);
		if (previous.isPresent()) {
			if (!previous.get().getFingerprint().equals(fingerprint)) {
				throw new IdempotencyKeyReusedException(idempotencyKey);
			}
			return previous.get().getResult();
		}
		BigDecimal result = operation.get();
		processedRequests.save(new ProcessedRequest(accountId, idempotencyKey, fingerprint, result));
		return result;
	}

	private static String fingerprint(Operation operation, Object... parts) {
		StringBuilder fingerprint = new StringBuilder(operation.name());
		for (Object part : parts) {
			fingerprint.append(' ').append(part instanceof BigDecimal amount
					? amount.stripTrailingZeros().toPlainString()
					: part);
		}
		return fingerprint.toString();
	}

	private Account findAccount(UUID accountId) {
		return accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
	}

	private Account findAccountForUpdate(UUID accountId) {
		return accounts.findForUpdateById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
	}

	private void requireSupported(Currency currency) {
		if (!exchangeRates.supports(currency)) {
			throw new UnsupportedCurrencyException(currency);
		}
	}

}
