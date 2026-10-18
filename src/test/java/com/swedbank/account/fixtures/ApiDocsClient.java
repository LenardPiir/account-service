package com.swedbank.account.fixtures;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.swedbank.account.fixtures.EndpointFixtures.API_DOCS_PATH;
import static com.swedbank.account.fixtures.EndpointFixtures.SWAGGER_UI_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public final class ApiDocsClient {

	private final MockMvcTester mvc;

	public ApiDocsClient(MockMvcTester mvc) {
		this.mvc = mvc;
	}

	public MvcTestResult swaggerUi() {
		return mvc.get().uri(SWAGGER_UI_PATH).exchange();
	}

	public Set<HttpStatus> documentedStatuses(HttpMethod method, String path) {
		Map<String, Object> responses = read("$.paths['%s'].%s.responses"
				.formatted(path, method.name().toLowerCase(Locale.ROOT)));
		return responses.keySet().stream()
				.map(code -> HttpStatus.valueOf(Integer.parseInt(code)))
				.collect(Collectors.toSet());
	}

	public Set<String> documentedFields(Class<?> schema) {
		Map<String, Object> properties = read("$.components.schemas.%s.properties".formatted(schema.getSimpleName()));
		return properties.keySet();
	}

	public String documentedType(Class<?> schema, String field) {
		return read("$.components.schemas.%s.properties.%s.type".formatted(schema.getSimpleName(), field));
	}

	private <T> T read(String jsonPath) {
		MvcTestResult result = mvc.get().uri(API_DOCS_PATH).exchange();
		assertThat(result).hasStatusOk();
		String json = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
		return JsonPath.read(json, jsonPath);
	}

}
