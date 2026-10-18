package com.swedbank.account.api;

import com.swedbank.account.domain.AccountNotFoundException;
import com.swedbank.account.domain.InsufficientFundsException;
import com.swedbank.account.domain.InvalidAmountException;
import com.swedbank.account.domain.UnsupportedCurrencyException;
import com.swedbank.account.external.ExternalSystemException;
import com.swedbank.account.service.IdempotencyKeyReusedException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@NullMarked
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(AccountNotFoundException.class)
	ProblemDetail accountNotFound(AccountNotFoundException e) {
		return problem(HttpStatus.NOT_FOUND, "Account not found", e.getMessage());
	}

	@ExceptionHandler(InsufficientFundsException.class)
	ProblemDetail insufficientFunds(InsufficientFundsException e) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient funds", e.getMessage());
	}

	@ExceptionHandler(UnsupportedCurrencyException.class)
	ProblemDetail unsupportedCurrency(UnsupportedCurrencyException e) {
		return problem(HttpStatus.BAD_REQUEST, "Unsupported currency", e.getMessage());
	}

	@ExceptionHandler(InvalidAmountException.class)
	ProblemDetail invalidAmount(InvalidAmountException e) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid amount", e.getMessage());
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail concurrentUpdate() {
		return problem(HttpStatus.CONFLICT, "Concurrent update",
				"The account was changed by another request, try again");
	}

	@ExceptionHandler(IdempotencyKeyReusedException.class)
	ProblemDetail idempotencyKeyReused(IdempotencyKeyReusedException e) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Idempotency key reused", e.getMessage());
	}

	@ExceptionHandler(ExternalSystemException.class)
	ProblemDetail externalSystemUnavailable(ExternalSystemException e) {
		log.warn("External system call failed", e);
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "External system unavailable",
				"Could not reach the external logging system, try again later");
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(ex,
				ProblemDetail.forStatusAndDetail(status, validationErrors(ex.getBindingResult().getAllErrors())),
				headers, status, request);
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
			HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(status, validationErrors(ex.getAllErrors())),
				headers, status, request);
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(status, unreadableBodyDetail(ex)),
				headers, status, request);
	}

	private static String validationErrors(List<? extends MessageSourceResolvable> errors) {
		return errors.stream()
				.map(MessageSourceResolvable::getDefaultMessage)
				.filter(Objects::nonNull)
				.sorted()
				.collect(Collectors.joining("; "));
	}

	private static String unreadableBodyDetail(HttpMessageNotReadableException ex) {
		if (ex.getCause() instanceof InvalidFormatException invalid) {
			String field = invalid.getPath().stream()
					.map(JacksonException.Reference::getPropertyName)
					.collect(Collectors.joining("."));
			return "Invalid value '" + invalid.getValue() + "' for " + field;
		}
		return "Malformed JSON request";
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

}
