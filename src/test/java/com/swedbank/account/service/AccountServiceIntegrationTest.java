package com.swedbank.account.service;

import com.swedbank.account.TestcontainersConfiguration;
import com.swedbank.account.domain.Account;
import com.swedbank.account.domain.InsufficientFundsException;
import com.swedbank.account.external.ExternalLoggingClient;
import com.swedbank.account.fixtures.ConcurrencyFixtures.Outcomes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static com.swedbank.account.fixtures.AccountFixtures.OWNER_NAME;
import static com.swedbank.account.fixtures.AmountFixtures.amount;
import static com.swedbank.account.fixtures.ConcurrencyFixtures.runConcurrently;
import static com.swedbank.account.fixtures.ConcurrencyFixtures.slowDownExternalLogging;
import static com.swedbank.account.fixtures.CurrencyFixtures.EUR;
import static com.swedbank.account.fixtures.CurrencyFixtures.USD;
import static com.swedbank.account.fixtures.ExchangeRateFixtures.USD_RATE;
import static com.swedbank.account.fixtures.IdempotencyFixtures.NO_KEY;
import static com.swedbank.account.fixtures.IdempotencyFixtures.aKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "exchange.rates.USD=" + USD_RATE)
@Import(TestcontainersConfiguration.class)
class AccountServiceIntegrationTest {

	@Autowired
	private AccountService service;

	@MockitoBean
	private ExternalLoggingClient externalLogging;

	@Test
	void operationsArePersistedToTheDatabase() {
		Account account = service.createAccount(OWNER_NAME);

		service.credit(account.getId(), EUR, amount("100.00"), NO_KEY);
		service.debit(account.getId(), EUR, amount("30.00"), NO_KEY);
		service.exchange(account.getId(), EUR, USD, amount("50.00"), NO_KEY);

		Map<Currency, BigDecimal> balances = service.balances(account.getId());
		assertThat(balances.get(EUR)).isEqualByComparingTo("20.00");
		assertThat(balances.get(USD)).isEqualByComparingTo("55.00");
		verify(externalLogging).logDebit(account.getId(), EUR, amount("30.00"));
	}

	@Test
	void concurrentDebitsNeitherOverdrawNorLoseUpdates() throws Exception {
		Account account = createAccountWith(EUR, "100.00");
		slowDownExternalLogging(externalLogging);

		Outcomes<BigDecimal> outcomes = runConcurrently(20,
				() -> service.debit(account.getId(), EUR, amount("10.00"), NO_KEY));

		assertThat(outcomes.results()).hasSize(10);
		assertThat(outcomes.failures()).hasSize(10).allMatch(InsufficientFundsException.class::isInstance);
		assertThat(service.balances(account.getId()).get(EUR)).isEqualByComparingTo("0.00");
		verify(externalLogging, times(10)).logDebit(account.getId(), EUR, amount("10.00"));
	}

	@Test
	void concurrentRetriesWithSameKeyAreAppliedOnce() throws Exception {
		Account account = createAccountWith(EUR, "100.00");
		slowDownExternalLogging(externalLogging);
		String key = aKey();

		Outcomes<BigDecimal> outcomes = runConcurrently(10,
				() -> service.debit(account.getId(), EUR, amount("10.00"), key));

		assertThat(outcomes.failures()).isEmpty();
		assertThat(outcomes.results()).hasSize(10)
				.allSatisfy(balance -> assertThat(balance).isEqualByComparingTo("90.00"));
		assertThat(service.balances(account.getId()).get(EUR)).isEqualByComparingTo("90.00");
		verify(externalLogging, times(1)).logDebit(account.getId(), EUR, amount("10.00"));
	}

	private Account createAccountWith(Currency currency, String balance) {
		Account account = service.createAccount(OWNER_NAME);
		service.credit(account.getId(), currency, amount(balance), NO_KEY);
		return account;
	}

}
