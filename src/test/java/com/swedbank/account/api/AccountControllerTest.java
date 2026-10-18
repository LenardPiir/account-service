package com.swedbank.account.api;

import com.swedbank.account.domain.Account;
import com.swedbank.account.domain.AccountNotFoundException;
import com.swedbank.account.domain.InsufficientFundsException;
import com.swedbank.account.domain.InvalidAmountException;
import com.swedbank.account.domain.UnsupportedCurrencyException;
import com.swedbank.account.external.ExternalSystemException;
import com.swedbank.account.fixtures.AccountApiClient;
import com.swedbank.account.service.AccountService;
import com.swedbank.account.service.IdempotencyKeyReusedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.swedbank.account.fixtures.AccountFixtures.OWNER_NAME;
import static com.swedbank.account.fixtures.AccountFixtures.anAccount;
import static com.swedbank.account.fixtures.AmountFixtures.amount;
import static com.swedbank.account.fixtures.CurrencyFixtures.EUR;
import static com.swedbank.account.fixtures.CurrencyFixtures.INVALID_CURRENCY_CODE;
import static com.swedbank.account.fixtures.CurrencyFixtures.JPY;
import static com.swedbank.account.fixtures.CurrencyFixtures.USD;
import static com.swedbank.account.fixtures.EndpointFixtures.accountUrl;
import static com.swedbank.account.fixtures.IdempotencyFixtures.MAX_KEY_LENGTH;
import static com.swedbank.account.fixtures.IdempotencyFixtures.NO_KEY;
import static com.swedbank.account.fixtures.IdempotencyFixtures.aKey;
import static com.swedbank.account.fixtures.IdempotencyFixtures.aKeyTooLong;
import static com.swedbank.account.fixtures.JsonFixtures.MALFORMED_JSON;
import static com.swedbank.account.fixtures.JsonFixtures.aValidMoneyJson;
import static com.swedbank.account.fixtures.JsonFixtures.accountJson;
import static com.swedbank.account.fixtures.JsonFixtures.balanceJson;
import static com.swedbank.account.fixtures.JsonFixtures.balancesJson;
import static com.swedbank.account.fixtures.JsonFixtures.createAccountJson;
import static com.swedbank.account.fixtures.JsonFixtures.exchangeJson;
import static com.swedbank.account.fixtures.JsonFixtures.exchangeJsonWithoutFrom;
import static com.swedbank.account.fixtures.JsonFixtures.exchangeJsonWithoutTo;
import static com.swedbank.account.fixtures.JsonFixtures.exchangeResultJson;
import static com.swedbank.account.fixtures.JsonFixtures.moneyJson;
import static com.swedbank.account.fixtures.JsonFixtures.moneyJsonWithoutAmount;
import static com.swedbank.account.fixtures.JsonFixtures.moneyJsonWithoutCurrency;
import static com.swedbank.account.fixtures.JsonFixtures.problemJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(AccountController.class)
@Import(AccountApiClient.class)
class AccountControllerTest {

	@Autowired
	private AccountApiClient api;

	@MockitoBean
	private AccountService service;

	private final UUID accountId = UUID.randomUUID();

	@Test
	void createAccountReturnsCreatedWithLocation() {
		Account account = anAccount();
		when(service.createAccount(OWNER_NAME)).thenReturn(account);

		assertThat(api.createAccount(createAccountJson(OWNER_NAME)))
				.hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", accountUrl(account.getId()))
				.bodyJson().isLenientlyEqualTo(accountJson(account.getId(), OWNER_NAME));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void createAccountWithBlankOwnerNameIsBadRequest(String ownerName) {
		assertThat(api.createAccount(createAccountJson(ownerName)))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(400, "Bad Request", "ownerName must not be blank"));

		verifyNoInteractions(service);
	}

	@Test
	void getAccountReturnsIdAndOwner() {
		Account account = anAccount();
		when(service.getAccount(account.getId())).thenReturn(account);

		assertThat(api.getAccount(account.getId()))
				.hasStatusOk()
				.bodyJson().isLenientlyEqualTo(accountJson(account.getId(), OWNER_NAME));
	}

	@Test
	void creditReturnsNewBalance() {
		when(service.credit(accountId, EUR, amount("100.00"), NO_KEY)).thenReturn(amount("100.00"));

		assertThat(api.credit(accountId, moneyJson(EUR, "100.00")))
				.hasStatusOk()
				.bodyJson().isLenientlyEqualTo(balanceJson(EUR, "100.00"));
	}

	@Test
	void debitReturnsNewBalance() {
		when(service.debit(accountId, EUR, amount("30.00"), NO_KEY)).thenReturn(amount("70.00"));

		assertThat(api.debit(accountId, moneyJson(EUR, "30.00")))
				.hasStatusOk()
				.bodyJson().isLenientlyEqualTo(balanceJson(EUR, "70.00"));
	}

	@Test
	void balanceIsShownWithTheCurrencyDecimalPlaces() {
		when(service.credit(accountId, EUR, amount("100.00"), NO_KEY)).thenReturn(amount("100.0000"));

		assertThat(api.credit(accountId, moneyJson(EUR, "100.00")))
				.hasStatusOk()
				.bodyText().isEqualTo(balanceJson(EUR, "100.00"));
	}

	@Test
	void exchangeReturnsDebitedAndCreditedAmounts() {
		when(service.exchange(accountId, EUR, USD, amount("50"), NO_KEY)).thenReturn(amount("55.98"));

		assertThat(api.exchange(accountId, exchangeJson(EUR, USD, "50")))
				.hasStatusOk()
				.bodyText().isEqualTo(exchangeResultJson(EUR, USD, "50.00", "55.98"));
	}

	@Test
	void balancesListsEveryCurrencyInTheCurrencyScale() {
		Map<Currency, BigDecimal> balances = new LinkedHashMap<>();
		balances.put(EUR, amount("20.0000"));
		balances.put(USD, BigDecimal.ZERO);
		when(service.balances(accountId)).thenReturn(balances);

		assertThat(api.balances(accountId))
				.hasStatusOk()
				.bodyText().isEqualTo(balancesJson(accountId, balanceJson(EUR, "20.00"), balanceJson(USD, "0.00")));
	}

	@Test
	void unknownAccountIsNotFound() {
		when(service.debit(any(), any(), any(), any())).thenThrow(new AccountNotFoundException(accountId));

		assertThat(api.debit(accountId, aValidMoneyJson()))
				.hasStatus(HttpStatus.NOT_FOUND)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(404, "Account not found", "Account not found: " + accountId));
	}

	@Test
	void insufficientFundsIsUnprocessable() {
		when(service.debit(any(), any(), any(), any())).thenThrow(new InsufficientFundsException(EUR));

		assertThat(api.debit(accountId, aValidMoneyJson()))
				.hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(422, "Insufficient funds", "Insufficient funds in EUR"));
	}

	@Test
	void externalSystemFailureIsServiceUnavailable() {
		when(service.debit(any(), any(), any(), any()))
				.thenThrow(new ExternalSystemException("Connection refused: internal-host:8080"));

		assertThat(api.debit(accountId, aValidMoneyJson()))
				.hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(503, "External system unavailable",
						"Could not reach the external logging system, try again later"));
	}

	@Test
	void unsupportedCurrencyIsBadRequest() {
		when(service.debit(any(), any(), any(), any())).thenThrow(new UnsupportedCurrencyException(JPY));

		assertThat(api.debit(accountId, aValidMoneyJson()))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(400, "Unsupported currency", "Currency not supported: JPY"));
	}

	@Test
	void invalidAmountIsBadRequest() {
		when(service.debit(any(), any(), any(), any()))
				.thenThrow(new InvalidAmountException("Amount in EUR must have at most 2 decimal places"));

		assertThat(api.debit(accountId, aValidMoneyJson()))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(400, "Invalid amount",
						"Amount in EUR must have at most 2 decimal places"));
	}

	@Test
	void concurrentUpdateIsConflict() {
		when(service.debit(any(), any(), any(), any()))
				.thenThrow(new ObjectOptimisticLockingFailureException(Account.class, accountId));

		assertThat(api.debit(accountId, aValidMoneyJson()))
				.hasStatus(HttpStatus.CONFLICT)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(409, "Concurrent update",
						"The account was changed by another request, try again"));
	}

	@Test
	void idempotencyKeyIsPassedToTheService() {
		String key = aKey();
		when(service.debit(accountId, EUR, amount("30.00"), key)).thenReturn(amount("70.00"));

		assertThat(api.debit(accountId, moneyJson(EUR, "30.00"), key))
				.hasStatusOk()
				.bodyJson().isLenientlyEqualTo(balanceJson(EUR, "70.00"));
	}

	@Test
	void reusedIdempotencyKeyIsUnprocessable() {
		String key = aKey();
		when(service.debit(any(), any(), any(), any())).thenThrow(new IdempotencyKeyReusedException(key));

		assertThat(api.debit(accountId, aValidMoneyJson(), key))
				.hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(422, "Idempotency key reused",
						"Idempotency key %s was already used for a different request".formatted(key)));
	}

	@Test
	void tooLongIdempotencyKeyIsRejectedBeforeReachingTheService() {
		assertThat(api.debit(accountId, aValidMoneyJson(), aKeyTooLong()))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(400, "Bad Request",
						"Idempotency-Key must be at most %d characters".formatted(MAX_KEY_LENGTH)));

		verifyNoInteractions(service);
	}

	static Stream<Arguments> invalidDebitRequests() {
		return Stream.of(
				arguments(named("missing currency", moneyJsonWithoutCurrency("10.00")), "currency is required"),
				arguments(named("missing amount", moneyJsonWithoutAmount(EUR)), "amount is required"),
				arguments(named("zero amount", moneyJson(EUR, "0")), "amount must be greater than 0"),
				arguments(named("negative amount", moneyJson(EUR, "-5.00")), "amount must be greater than 0"),
				arguments(named("several problems", moneyJsonWithoutCurrency("-5.00")),
						"amount must be greater than 0; currency is required"),
				arguments(named("invalid ISO code", moneyJson(INVALID_CURRENCY_CODE, "10.00")),
						"Invalid value '%s' for currency".formatted(INVALID_CURRENCY_CODE)),
				arguments(named("malformed JSON", MALFORMED_JSON), "Malformed JSON request"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidDebitRequests")
	void invalidDebitIsRejectedWithExplanationBeforeReachingTheService(String json, String detail) {
		assertThat(api.debit(accountId, json))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(400, "Bad Request", detail));

		verifyNoInteractions(service);
	}

	static Stream<Arguments> invalidExchangeRequests() {
		return Stream.of(
				arguments(named("same currency", exchangeJson(EUR, EUR, "5")), "from and to must be different currencies"),
				arguments(named("missing from", exchangeJsonWithoutFrom(USD, "5")), "from is required"),
				arguments(named("missing to", exchangeJsonWithoutTo(EUR, "5")), "to is required"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidExchangeRequests")
	void invalidExchangeIsRejectedWithExplanationBeforeReachingTheService(String json, String detail) {
		assertThat(api.exchange(accountId, json))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo(problemJson(400, "Bad Request", detail));

		verifyNoInteractions(service);
	}

}
