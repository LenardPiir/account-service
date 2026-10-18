package com.swedbank.account.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(@NotBlank(message = "ownerName must not be blank") String ownerName) {
}
