package com.swedbank.account.service;

import com.swedbank.account.persistence.ProcessedRequest;
import com.swedbank.account.persistence.ProcessedRequestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class InMemoryProcessedRequestRepository implements ProcessedRequestRepository {

	private final List<ProcessedRequest> processedRequests = new ArrayList<>();

	@Override
	public Optional<ProcessedRequest> findByAccountIdAndIdempotencyKey(UUID accountId, String idempotencyKey) {
		return processedRequests.stream()
				.filter(request -> request.getAccountId().equals(accountId))
				.filter(request -> request.getIdempotencyKey().equals(idempotencyKey))
				.findFirst();
	}

	@Override
	public ProcessedRequest save(ProcessedRequest processedRequest) {
		processedRequests.add(processedRequest);
		return processedRequest;
	}

}
