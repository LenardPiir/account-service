package com.swedbank.account.exchange;

import com.swedbank.account.domain.UnsupportedCurrencyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static com.swedbank.account.fixtures.AmountFixtures.amount;
import static com.swedbank.account.fixtures.CurrencyFixtures.EUR;
import static com.swedbank.account.fixtures.CurrencyFixtures.GBP;
import static com.swedbank.account.fixtures.CurrencyFixtures.JPY;
import static com.swedbank.account.fixtures.CurrencyFixtures.SEK;
import static com.swedbank.account.fixtures.CurrencyFixtures.SUPPORTED_CURRENCIES;
import static com.swedbank.account.fixtures.CurrencyFixtures.USD;
import static com.swedbank.account.fixtures.ExchangeRateFixtures.testExchangeRates;
import static com.swedbank.account.fixtures.ExchangeRateFixtures.testRates;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRatesTest {

	private final ExchangeRates rates = testExchangeRates();

	@Test
	void convertingToSameCurrencyReturnsAmountUnchanged() {
		BigDecimal result = rates.convert(amount("100.00"), USD, USD);

		assertThat(result).isEqualByComparingTo("100.00");
	}

	@Test
	void convertsFromEurUsingRate() {
		BigDecimal result = rates.convert(amount("100.00"), EUR, USD);

		assertThat(result).isEqualByComparingTo("110.00");
	}

	@Test
	void convertsToEurUsingRate() {
		BigDecimal result = rates.convert(amount("110.00"), USD, EUR);

		assertThat(result).isEqualByComparingTo("100.00");
	}

	@Test
	void convertsBetweenNonEurCurrenciesThroughEur() {
		BigDecimal result = rates.convert(amount("100.00"), USD, SEK);

		assertThat(result).isEqualByComparingTo("1000.00");
	}

	@Test
	void roundsResultToTwoDecimalPlaces() {
		BigDecimal result = rates.convert(amount("100.00"), USD, EUR);

		assertThat(result).isEqualByComparingTo("90.91");
	}

	@Test
	void roundsExactHalfUp() {
		BigDecimal result = rates.convert(amount("0.15"), EUR, USD);

		assertThat(result).isEqualByComparingTo("0.17");
	}

	@ParameterizedTest
	@ValueSource(strings = {"0", "-1.10"})
	void rejectsNonPositiveRate(String rate) {
		Map<Currency, BigDecimal> rates = testRates();
		rates.put(USD, amount(rate));

		assertThatThrownBy(() -> new ExchangeRates(rates))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("USD");
	}

	@Test
	void supportsOnlyCurrenciesWithRate() {
		assertThat(rates.supports(EUR)).isTrue();
		assertThat(rates.supports(GBP)).isTrue();
		assertThat(rates.supports(JPY)).isFalse();
	}

	@Test
	void supportedCurrenciesAreThoseWithRate() {
		assertThat(rates.supportedCurrencies()).containsExactlyInAnyOrderElementsOf(SUPPORTED_CURRENCIES);
	}

	@Test
	void convertRejectsUnsupportedCurrency() {
		assertThatThrownBy(() -> rates.convert(amount("100.00"), EUR, JPY))
				.isInstanceOf(UnsupportedCurrencyException.class)
				.hasMessageContaining("JPY");
		assertThatThrownBy(() -> rates.convert(amount("100"), JPY, EUR))
				.isInstanceOf(UnsupportedCurrencyException.class)
				.hasMessageContaining("JPY");
	}

	@Test
	void roundsToDecimalPlacesOfTargetCurrency() {
		Map<Currency, BigDecimal> withJpy = testRates();
		withJpy.put(JPY, amount("160"));

		BigDecimal result = new ExchangeRates(withJpy).convert(amount("0.01"), EUR, JPY);

		assertThat(result).isEqualByComparingTo("2");
	}

}
