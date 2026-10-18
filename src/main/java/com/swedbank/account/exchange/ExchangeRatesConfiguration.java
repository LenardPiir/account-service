package com.swedbank.account.exchange;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExchangeRatesProperties.class)
public class ExchangeRatesConfiguration {

	@Bean
	ExchangeRates exchangeRates(ExchangeRatesProperties properties) {
		return new ExchangeRates(properties.rates());
	}

}
