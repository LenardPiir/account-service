package com.swedbank.account.exchange;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

@ConfigurationProperties(prefix = "exchange")
public record ExchangeRatesProperties(Map<Currency, BigDecimal> rates) {
}
