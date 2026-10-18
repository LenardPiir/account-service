package com.swedbank.account.persistence;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedRequestRepository extends Repository<ProcessedRequest, UUID> {

	Optional<ProcessedRequest> findByAccountIdAndIdempotencyKey(UUID accountId, String idempotencyKey);

	ProcessedRequest save(ProcessedRequest processedRequest);

}
