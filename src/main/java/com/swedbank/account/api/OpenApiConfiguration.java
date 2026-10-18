package com.swedbank.account.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Currency;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

	static final String BAD_REQUEST = "#/components/responses/BadRequest";
	static final String NOT_FOUND = "#/components/responses/NotFound";
	static final String CONFLICT = "#/components/responses/Conflict";
	static final String INSUFFICIENT_FUNDS_OR_KEY_REUSED = "#/components/responses/InsufficientFundsOrKeyReused";
	static final String IDEMPOTENCY_KEY_REUSED = "#/components/responses/IdempotencyKeyReused";
	static final String EXTERNAL_SYSTEM_UNAVAILABLE = "#/components/responses/ExternalSystemUnavailable";

	static {
		SpringDocUtils.getConfig().replaceWithSchema(Currency.class,
				new StringSchema().example("EUR").description("ISO 4217 currency code"));
	}

	@Bean
	OpenAPI accountServiceOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Account Service API")
						.version("v1")
						.description("Multi-currency accounts: create, credit, debit, balances and currency exchange"))
				.components(new Components()
						.addSchemas("ProblemDetail", problemDetailSchema())
						.addResponses("BadRequest", problem("Invalid request, invalid amount or unsupported currency"))
						.addResponses("NotFound", problem("Account not found"))
						.addResponses("Conflict", problem("Account changed by a concurrent request, retry"))
						.addResponses("InsufficientFundsOrKeyReused", problem(
								"Not enough money in the currency, or Idempotency-Key already used for a different request"))
						.addResponses("IdempotencyKeyReused", problem("Idempotency-Key already used for a different request"))
						.addResponses("ExternalSystemUnavailable",
								problem("External logging system unavailable, nothing was debited")));
	}

	private static Schema<?> problemDetailSchema() {
		return new ObjectSchema()
				.addProperty("type", new StringSchema().format("uri"))
				.addProperty("title", new StringSchema())
				.addProperty("status", new IntegerSchema())
				.addProperty("detail", new StringSchema())
				.addProperty("instance", new StringSchema().format("uri"));
	}

	private static ApiResponse problem(String description) {
		return new ApiResponse()
				.description(description)
				.content(new Content().addMediaType("application/problem+json",
						new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
	}

}
