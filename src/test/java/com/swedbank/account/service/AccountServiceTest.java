package com.swedbank.account.service;

import com.swedbank.account.domain.Account;
import com.swedbank.account.domain.AccountNotFoundException;
import com.swedbank.account.domain.InsufficientFundsException;
import com.swedbank.account.domain.UnsupportedCurrencyException;
import com.swedbank.account.external.ExternalLoggingClient;
import com.swedbank.account.external.ExternalSystemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;
import java.util.UUID;

import static com.swedbank.account.fixtures.AccountFixtures.OWNER_NAME;
import static com.swedbank.account.fixtures.AccountFixtures.anAccount;
import static com.swedbank.account.fixtures.AccountFixtures.anAccountWith;
import static com.swedbank.account.fixtures.AmountFixtures.amount;
import static com.swedbank.account.fixtures.CurrencyFixtures.EUR;
import static com.swedbank.account.fixtures.CurrencyFixtures.JPY;
import static com.swedbank.account.fixtures.CurrencyFixtures.SUPPORTED_CURRENCIES;
import static com.swedbank.account.fixtures.CurrencyFixtures.USD;
import static com.swedbank.account.fixtures.ExchangeRateFixtures.testExchangeRates;
import static com.swedbank.account.fixtures.IdempotencyFixtures.NO_KEY;
import static com.swedbank.account.fixtures.IdempotencyFixtures.aKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AccountServiceTest {

	private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
	private final ExternalLoggingClient externalLogging = mock(ExternalLoggingClient.class);
	private final AccountService service = new AccountService(accounts, testExchangeRates(), externalLogging,
			new InMemoryProcessedRequestRepository());

	@Test
	void getAccountReturnsStoredAccount() {
		Account account = accounts.save(anAccount());

		assertThat(service.getAccount(account.getId())).isSameAs(account);
	}

	@Test
	void getAccountForUnknownIdIsRejected() {
		UUID unknownId = UUID.randomUUID();

		assertThatThrownBy(() -> service.getAccount(unknownId))
				.isInstanceOf(AccountNotFoundException.class);
	}

	@Test
	void creditAddsMoneyToExistingAccount() {
		Account account = accounts.save(anAccount());

		BigDecimal balance = service.credit(account.getId(), EUR, amount("100.00"), NO_KEY);

		assertThat(balance).isEqualByComparingTo("100.00");
		assertThat(accounts.findById(account.getId()).orElseThrow().balanceOf(EUR)).isEqualByComparingTo("100.00");
	}

	@Test
	void creditToUnknownAccountIsRejected() {
		UUID unknownId = UUID.randomUUID();

		assertThatThrownBy(() -> service.credit(unknownId, EUR, amount("100.00"), NO_KEY))
				.isInstanceOf(AccountNotFoundException.class)
				.hasMessageContaining(unknownId.toString());
	}

	@Test
	void creditInUnsupportedCurrencyIsRejectedAndNothingChanges() {
		Account account = accounts.save(anAccount());

		assertThatThrownBy(() -> service.credit(account.getId(), JPY, amount("100"), NO_KEY))
				.isInstanceOf(UnsupportedCurrencyException.class);

		assertThat(account.balanceOf(JPY)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void debitNotifiesExternalSystemAndReducesBalance() {
		Account account = accounts.save(anAccountWith(EUR, "100.00"));

		BigDecimal balance = service.debit(account.getId(), EUR, amount("30.00"), NO_KEY);

		assertThat(balance).isEqualByComparingTo("70.00");
		verify(externalLogging).logDebit(account.getId(), EUR, amount("30.00"));
	}

	@Test
	void debitIsRejectedAndBalanceUnchangedWhenExternalSystemFails() {
		Account account = accounts.save(anAccountWith(EUR, "100.00"));
		doThrow(new ExternalSystemException("unavailable"))
				.when(externalLogging).logDebit(any(), any(), any());

		assertThatThrownBy(() -> service.debit(account.getId(), EUR, amount("30.00"), NO_KEY))
				.isInstanceOf(ExternalSystemException.class);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("100.00");
	}

	@Test
	void debitWithInsufficientFundsDoesNotCallExternalSystem() {
		Account account = accounts.save(anAccountWith(EUR, "10.00"));

		assertThatThrownBy(() -> service.debit(account.getId(), EUR, amount("10.01"), NO_KEY))
				.isInstanceOf(InsufficientFundsException.class);

		verifyNoInteractions(externalLogging);
	}

	@Test
	void exchangeConvertsUsingRatesAndReturnsCreditedAmount() {
		Account account = accounts.save(anAccountWith(EUR, "100.00"));

		BigDecimal credited = service.exchange(account.getId(), EUR, USD, amount("50.00"), NO_KEY);

		assertThat(credited).isEqualByComparingTo("55.00");
		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("50.00");
		assertThat(account.balanceOf(USD)).isEqualByComparingTo("55.00");
		verifyNoInteractions(externalLogging);
	}

	@Test
	void debitRetriedWithSameKeyIsAppliedOnce() {
		Account account = accounts.save(anAccountWith(EUR, "100.00"));
		String key = aKey();

		BigDecimal first = service.debit(account.getId(), EUR, amount("30.00"), key);
		BigDecimal retry = service.debit(account.getId(), EUR, amount("30.00"), key);

		assertThat(first).isEqualByComparingTo("70.00");
		assertThat(retry).isEqualByComparingTo("70.00");
		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("70.00");
		verify(externalLogging, times(1)).logDebit(account.getId(), EUR, amount("30.00"));
	}

	@Test
	void creditRetriedWithSameKeyIsAppliedOnce() {
		Account account = accounts.save(anAccount());
		String key = aKey();

		service.credit(account.getId(), EUR, amount("100.00"), key);
		BigDecimal retry = service.credit(account.getId(), EUR, amount("100.00"), key);

		assertThat(retry).isEqualByComparingTo("100.00");
		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("100.00");
	}

	@Test
	void exchangeRetriedWithSameKeyIsAppliedOnce() {
		Account account = accounts.save(anAccountWith(EUR, "100.00"));
		String key = aKey();

		service.exchange(account.getId(), EUR, USD, amount("50.00"), key);
		BigDecimal retry = service.exchange(account.getId(), EUR, USD, amount("50.00"), key);

		assertThat(retry).isEqualByComparingTo("55.00");
		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("50.00");
		assertThat(account.balanceOf(USD)).isEqualByComparingTo("55.00");
	}

	@Test
	void retryWithSameAmountWrittenDifferentlyIsStillTheSameRequest() {
		Account account = accounts.save(anAccountWith(EUR, "100.00"));
		String key = aKey();

		service.debit(account.getId(), EUR, amount("30.00"), key);
		service.debit(account.getId(), EUR, amount("30"), key);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("70.00");
	}

	@Test
	void sameKeyForDifferentRequestIsRejectedAndNothingChanges() {
		Account account = accounts.save(anAccountWith(EUR, "100.00"));
		String key = aKey();
		service.debit(account.getId(), EUR, amount("30.00"), key);

		assertThatThrownBy(() -> service.debit(account.getId(), EUR, amount("40.00"), key))
				.isInstanceOf(IdempotencyKeyReusedException.class);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("70.00");
	}

	@Test
	void sameKeyOnAnotherAccountIsASeparateRequest() {
		Account first = accounts.save(anAccountWith(EUR, "100.00"));
		Account second = accounts.save(anAccountWith(EUR, "100.00"));
		String key = aKey();

		service.debit(first.getId(), EUR, amount("30.00"), key);
		service.debit(second.getId(), EUR, amount("30.00"), key);

		assertThat(first.balanceOf(EUR)).isEqualByComparingTo("70.00");
		assertThat(second.balanceOf(EUR)).isEqualByComparingTo("70.00");
	}

	@Test
	void requestsWithoutKeyAreAppliedEveryTime() {
		Account account = accounts.save(anAccountWith(EUR, "100.00"));

		service.debit(account.getId(), EUR, amount("30.00"), NO_KEY);
		service.debit(account.getId(), EUR, amount("30.00"), NO_KEY);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("40.00");
	}

	@Test
	void failedRequestIsNotRememberedSoItCanBeRetried() {
		Account account = accounts.save(anAccountWith(EUR, "10.00"));
		String key = aKey();
		assertThatThrownBy(() -> service.debit(account.getId(), EUR, amount("30.00"), key))
				.isInstanceOf(InsufficientFundsException.class);
		account.credit(EUR, amount("50.00"));

		BigDecimal balance = service.debit(account.getId(), EUR, amount("30.00"), key);

		assertThat(balance).isEqualByComparingTo("30.00");
	}

	@Test
	void createdAccountCanBeFoundAndStartsEmpty() {
		Account account = service.createAccount(OWNER_NAME);

		Account stored = accounts.findById(account.getId()).orElseThrow();
		assertThat(stored.getOwnerName()).isEqualTo(OWNER_NAME);
		assertThat(stored.balanceOf(EUR)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void balancesListsEverySupportedCurrencyIncludingZero() {
		Account account = accounts.save(anAccountWith(EUR, "100.00"));

		Map<Currency, BigDecimal> balances = service.balances(account.getId());

		assertThat(balances).containsOnlyKeys(SUPPORTED_CURRENCIES);
		assertThat(balances.get(EUR)).isEqualByComparingTo("100.00");
		assertThat(balances.get(USD)).isEqualByComparingTo(BigDecimal.ZERO);
	}

}
