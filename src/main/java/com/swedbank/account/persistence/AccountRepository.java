package com.swedbank.account.persistence;

import com.swedbank.account.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends Repository<Account, UUID> {

	Optional<Account> findById(UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Account> findForUpdateById(UUID id);

	Account save(Account account);

}
