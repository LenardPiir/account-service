package com.swedbank.account.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static com.swedbank.account.fixtures.AmountFixtures.amount;
import static com.swedbank.account.fixtures.CurrencyFixtures.EUR;
import static com.swedbank.account.fixtures.CurrencyFixtures.USD;
import static com.swedbank.account.fixtures.ExchangeRateFixtures.EUR_RATE;
import static com.swedbank.account.fixtures.ExchangeRateFixtures.USD_RATE;
import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRatesConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(ExchangeRatesConfiguration.class);

	@Test
	void createsExchangeRatesFromProperties() {
		contextRunner
				.withPropertyValues("exchange.rates.EUR=" + EUR_RATE, "exchange.rates.USD=" + USD_RATE)
				.run(context -> {
					ExchangeRates rates = context.getBean(ExchangeRates.class);

					assertThat(rates.supportedCurrencies()).containsExactlyInAnyOrder(EUR, USD);
					assertThat(rates.convert(amount("100.00"), EUR, USD)).isEqualByComparingTo("110.00");
				});
	}

	@Test
	void failsToStartWithInvalidRate() {
		contextRunner
				.withPropertyValues("exchange.rates.EUR=" + EUR_RATE, "exchange.rates.USD=0")
				.run(context -> assertThat(context).hasFailed());
	}

}
