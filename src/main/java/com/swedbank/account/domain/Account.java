package com.swedbank.account.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

	@Id
	@Getter
	private UUID id;

	@Getter
	private String ownerName;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "balance", joinColumns = @JoinColumn(name = "account_id"))
	@MapKeyColumn(name = "currency")
	@Column(name = "amount", precision = 19, scale = 4)
	private Map<Currency, BigDecimal> balances = new HashMap<>();

	@Version
	private Long version;

	public Account(String ownerName) {
		this.id = UUID.randomUUID();
		this.ownerName = ownerName;
	}

	public BigDecimal balanceOf(Currency currency) {
		return balances.getOrDefault(currency, BigDecimal.ZERO);
	}

	public void credit(Currency currency, BigDecimal amount) {
		requireValidAmount(currency, amount);
		balances.merge(currency, amount, BigDecimal::add);
	}

	public void requireCanDebit(Currency currency, BigDecimal amount) {
		requireValidAmount(currency, amount);
		if (balanceOf(currency).compareTo(amount) < 0) {
			throw new InsufficientFundsException(currency);
		}
	}

	public void debit(Currency currency, BigDecimal amount) {
		requireCanDebit(currency, amount);
		balances.put(currency, balanceOf(currency).subtract(amount));
	}

	public BigDecimal exchange(Currency from, Currency to, BigDecimal amount, CurrencyConverter converter) {
		if (from.equals(to)) {
			throw new IllegalArgumentException("Cannot exchange " + from + " to itself");
		}
		BigDecimal converted = converter.convert(amount, from, to);
		requireValidAmount(to, converted);
		debit(from, amount);
		credit(to, converted);
		return converted;
	}

	private static void requireValidAmount(Currency currency, BigDecimal amount) {
		if (amount.signum() <= 0) {
			throw new InvalidAmountException("Amount must be positive");
		}
		int maxDecimalPlaces = currency.getDefaultFractionDigits();
		if (amount.stripTrailingZeros().scale() > maxDecimalPlaces) {
			throw new InvalidAmountException(
					"Amount in " + currency + " must have at most " + maxDecimalPlaces + " decimal places");
		}
	}

}
