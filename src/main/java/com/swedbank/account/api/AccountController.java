package com.swedbank.account.api;

import com.swedbank.account.api.dto.*;
import com.swedbank.account.domain.Account;
import com.swedbank.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.swedbank.account.api.OpenApiConfiguration.*;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts")
@RequiredArgsConstructor
public class AccountController {

	private static final String IDEMPOTENCY_KEY = "Idempotency-Key";
	private static final int MAX_KEY_LENGTH = 64;
	private static final String KEY_TOO_LONG = "Idempotency-Key must be at most " + MAX_KEY_LENGTH + " characters";

	private final AccountService service;

	@PostMapping
	@Operation(summary = "Create an account")
	@ApiResponse(responseCode = "201", description = "Account created, Location header points to it")
	@ApiResponse(responseCode = "400", ref = BAD_REQUEST)
	public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
		Account account = service.createAccount(request.ownerName());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(account.getId())
				.toUri();
		return ResponseEntity.created(location).body(AccountResponse.from(account));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get an account")
	@ApiResponse(responseCode = "200", description = "The account")
	@ApiResponse(responseCode = "400", ref = BAD_REQUEST)
	@ApiResponse(responseCode = "404", ref = NOT_FOUND)
	public AccountResponse getAccount(@PathVariable UUID id) {
		return AccountResponse.from(service.getAccount(id));
	}

	@GetMapping("/{id}/balances")
	@Operation(summary = "Get balances", description = "Lists every supported currency, including zero balances")
	@ApiResponse(responseCode = "200", description = "Balances sorted by currency code")
	@ApiResponse(responseCode = "400", ref = BAD_REQUEST)
	@ApiResponse(responseCode = "404", ref = NOT_FOUND)
	public AccountBalancesResponse balances(@PathVariable UUID id) {
		List<BalanceResponse> balances = service.balances(id).entrySet().stream()
				.map(entry -> new BalanceResponse(entry.getKey(), entry.getValue()))
				.toList();
		return new AccountBalancesResponse(id, balances);
	}

	@PostMapping("/{id}/credits")
	@Operation(summary = "Add money")
	@ApiResponse(responseCode = "200", description = "New balance in the credited currency")
	@ApiResponse(responseCode = "400", ref = BAD_REQUEST)
	@ApiResponse(responseCode = "404", ref = NOT_FOUND)
	@ApiResponse(responseCode = "409", ref = CONFLICT)
	@ApiResponse(responseCode = "422", ref = IDEMPOTENCY_KEY_REUSED)
	public BalanceResponse credit(@PathVariable UUID id, @Valid @RequestBody MoneyRequest request,
			@RequestHeader(name = IDEMPOTENCY_KEY, required = false)
			@Size(max = MAX_KEY_LENGTH, message = KEY_TOO_LONG) String idempotencyKey) {
		BigDecimal balance = service.credit(id, request.currency(), request.amount(), idempotencyKey);
		return new BalanceResponse(request.currency(), balance);
	}

	@PostMapping("/{id}/debits")
	@Operation(summary = "Debit money",
			description = "Calls the external logging system first. Uses one currency only, no automatic exchange")
	@ApiResponse(responseCode = "200", description = "New balance in the debited currency")
	@ApiResponse(responseCode = "400", ref = BAD_REQUEST)
	@ApiResponse(responseCode = "404", ref = NOT_FOUND)
	@ApiResponse(responseCode = "409", ref = CONFLICT)
	@ApiResponse(responseCode = "422", ref = INSUFFICIENT_FUNDS_OR_KEY_REUSED)
	@ApiResponse(responseCode = "503", ref = EXTERNAL_SYSTEM_UNAVAILABLE)
	public BalanceResponse debit(@PathVariable UUID id, @Valid @RequestBody MoneyRequest request,
			@RequestHeader(name = IDEMPOTENCY_KEY, required = false)
			@Size(max = MAX_KEY_LENGTH, message = KEY_TOO_LONG) String idempotencyKey) {
		BigDecimal balance = service.debit(id, request.currency(), request.amount(), idempotencyKey);
		return new BalanceResponse(request.currency(), balance);
	}

	@PostMapping("/{id}/exchanges")
	@Operation(summary = "Exchange currency", description = "Converts at fixed rates, rounded half up")
	@ApiResponse(responseCode = "200", description = "Debited and credited amounts")
	@ApiResponse(responseCode = "400", ref = BAD_REQUEST)
	@ApiResponse(responseCode = "404", ref = NOT_FOUND)
	@ApiResponse(responseCode = "409", ref = CONFLICT)
	@ApiResponse(responseCode = "422", ref = INSUFFICIENT_FUNDS_OR_KEY_REUSED)
	public ExchangeResponse exchange(@PathVariable UUID id, @Valid @RequestBody ExchangeRequest request,
			@RequestHeader(name = IDEMPOTENCY_KEY, required = false)
			@Size(max = MAX_KEY_LENGTH, message = KEY_TOO_LONG) String idempotencyKey) {
		BigDecimal credited = service.exchange(id, request.from(), request.to(), request.amount(), idempotencyKey);
		return new ExchangeResponse(request.from(), request.to(), request.amount(), credited);
	}

}
