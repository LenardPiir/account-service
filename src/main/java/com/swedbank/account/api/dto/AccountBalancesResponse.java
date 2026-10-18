package com.swedbank.account.api.dto;

import java.util.List;
import java.util.UUID;

public record AccountBalancesResponse(UUID accountId, List<BalanceResponse> balances) {
}
