package com.swedbank.account.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private UUID accountId;

	private String idempotencyKey;

	private String fingerprint;

	@Column(precision = 19, scale = 4)
	private BigDecimal result;

	private Instant createdAt;

	public ProcessedRequest(UUID accountId, String idempotencyKey, String fingerprint, BigDecimal result) {
		this.accountId = accountId;
		this.idempotencyKey = idempotencyKey;
		this.fingerprint = fingerprint;
		this.result = result;
		this.createdAt = Instant.now();
	}

}
