package com.swedbank.account.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Currency;

import static com.swedbank.account.fixtures.AccountFixtures.anAccount;
import static com.swedbank.account.fixtures.AccountFixtures.anAccountWith;
import static com.swedbank.account.fixtures.AmountFixtures.amount;
import static com.swedbank.account.fixtures.CurrencyFixtures.EUR;
import static com.swedbank.account.fixtures.CurrencyFixtures.JPY;
import static com.swedbank.account.fixtures.CurrencyFixtures.SEK;
import static com.swedbank.account.fixtures.CurrencyFixtures.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

	@ParameterizedTest
	@FieldSource("com.swedbank.account.fixtures.CurrencyFixtures#SUPPORTED_CURRENCIES")
	void newAccountHasZeroBalance(Currency currency) {
		Account account = anAccount();

		assertThat(account.balanceOf(currency)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void creditIncreasesBalanceInThatCurrencyOnly() {
		Account account = anAccount();

		account.credit(EUR, amount("100.00"));

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("100.00");
		assertThat(account.balanceOf(USD)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void debitReducesBalance() {
		Account account = anAccountWith(EUR, "100.00");

		account.debit(EUR, amount("30.00"));

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("70.00");
	}

	@Test
	void debitMoreThanBalanceIsRejectedAndBalanceUnchanged() {
		Account account = anAccountWith(EUR, "50.00");

		assertThatThrownBy(() -> account.debit(EUR, amount("50.01")))
				.isInstanceOf(InsufficientFundsException.class);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("50.00");
	}

	@Test
	void debitOfWholeBalanceLeavesZero() {
		Account account = anAccountWith(EUR, "50.00");

		account.debit(EUR, amount("50.00"));

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void debitDoesNotUseOtherCurrencies() {
		Account account = anAccountWith(EUR, "100.00");

		assertThatThrownBy(() -> account.debit(USD, amount("10.00")))
				.isInstanceOf(InsufficientFundsException.class);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("100.00");
		assertThat(account.balanceOf(USD)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@ParameterizedTest
	@ValueSource(strings = {"0", "-0.01", "-100"})
	void creditRejectsNonPositiveAmount(String value) {
		Account account = anAccountWith(EUR, "100.00");

		assertThatThrownBy(() -> account.credit(EUR, amount(value)))
				.isInstanceOf(InvalidAmountException.class);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("100.00");
	}

	@ParameterizedTest
	@ValueSource(strings = {"0", "-0.01", "-100"})
	void debitRejectsNonPositiveAmount(String value) {
		Account account = anAccountWith(EUR, "100.00");

		assertThatThrownBy(() -> account.debit(EUR, amount(value)))
				.isInstanceOf(InvalidAmountException.class);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("100.00");
	}

	@ParameterizedTest
	@ValueSource(strings = {"0.001", "10.555", "100.0001"})
	void creditRejectsMoreThanTwoDecimalPlaces(String value) {
		Account account = anAccountWith(EUR, "100.00");

		assertThatThrownBy(() -> account.credit(EUR, amount(value)))
				.isInstanceOf(InvalidAmountException.class);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("100.00");
	}

	@ParameterizedTest
	@ValueSource(strings = {"0.001", "10.555", "100.0001"})
	void debitRejectsMoreThanTwoDecimalPlaces(String value) {
		Account account = anAccountWith(EUR, "100.00");

		assertThatThrownBy(() -> account.debit(EUR, amount(value)))
				.isInstanceOf(InvalidAmountException.class);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("100.00");
	}

	@Test
	void creditAcceptsTrailingZerosBeyondTwoDecimalPlaces() {
		Account account = anAccount();

		account.credit(EUR, amount("10.500"));

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("10.50");
	}

	@Test
	void decimalPlacesFollowTheCurrency() {
		Account account = anAccountWith(JPY, "100");

		assertThatThrownBy(() -> account.credit(JPY, amount("0.50")))
				.isInstanceOf(InvalidAmountException.class);

		assertThat(account.balanceOf(JPY)).isEqualByComparingTo("100");
	}

	@Test
	void exchangeDebitsSourceAndCreditsConvertedAmountToTarget() {
		Account account = anAccountWith(EUR, "100.00");
		CurrencyConverter doubling = (value, from, to) -> value.multiply(amount("2"));

		BigDecimal credited = account.exchange(EUR, USD, amount("40.00"), doubling);

		assertThat(credited).isEqualByComparingTo("80.00");
		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("60.00");
		assertThat(account.balanceOf(USD)).isEqualByComparingTo("80.00");
	}

	@Test
	void exchangeThatConvertsToZeroIsRejectedAndNothingChanges() {
		Account account = anAccountWith(SEK, "1.00");
		CurrencyConverter toZero = (value, from, to) -> amount("0.00");

		assertThatThrownBy(() -> account.exchange(SEK, EUR, amount("0.01"), toZero))
				.isInstanceOf(InvalidAmountException.class);

		assertThat(account.balanceOf(SEK)).isEqualByComparingTo("1.00");
		assertThat(account.balanceOf(EUR)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void exchangeWithInsufficientFundsCreditsNothing() {
		Account account = anAccountWith(EUR, "10.00");
		CurrencyConverter doubling = (value, from, to) -> value.multiply(amount("2"));

		assertThatThrownBy(() -> account.exchange(EUR, USD, amount("10.01"), doubling))
				.isInstanceOf(InsufficientFundsException.class);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("10.00");
		assertThat(account.balanceOf(USD)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void exchangeToSameCurrencyIsRejected() {
		Account account = anAccountWith(EUR, "100.00");
		CurrencyConverter identity = (value, from, to) -> value;

		assertThatThrownBy(() -> account.exchange(EUR, EUR, amount("10.00"), identity))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(account.balanceOf(EUR)).isEqualByComparingTo("100.00");
	}

}
