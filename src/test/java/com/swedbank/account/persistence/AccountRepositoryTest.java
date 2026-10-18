package com.swedbank.account.persistence;

import com.swedbank.account.TestcontainersConfiguration;
import com.swedbank.account.domain.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static com.swedbank.account.fixtures.AccountFixtures.OWNER_NAME;
import static com.swedbank.account.fixtures.AccountFixtures.anAccountWith;
import static com.swedbank.account.fixtures.AmountFixtures.amount;
import static com.swedbank.account.fixtures.CurrencyFixtures.EUR;
import static com.swedbank.account.fixtures.CurrencyFixtures.USD;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class AccountRepositoryTest {

	@Autowired
	private AccountRepository accounts;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savedAccountIsLoadedBackWithItsBalances() {
		Account account = anAccountWith(EUR, "100.00");
		account.credit(USD, amount("5.50"));
		accounts.save(account);
		entityManager.flush();
		entityManager.clear();

		Account loaded = accounts.findById(account.getId()).orElseThrow();

		assertThat(loaded).isNotSameAs(account);
		assertThat(loaded.getOwnerName()).isEqualTo(OWNER_NAME);
		assertThat(loaded.balanceOf(EUR)).isEqualByComparingTo("100.00");
		assertThat(loaded.balanceOf(USD)).isEqualByComparingTo("5.50");
	}

}
