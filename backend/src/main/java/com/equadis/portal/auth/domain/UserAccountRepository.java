package com.equadis.portal.auth.domain;

import java.util.Optional;

/** Persistence port for user accounts. Implemented by Spring Data in infrastructure. */
public interface UserAccountRepository {

	/** Saves and flushes, so the unique username index is checked immediately. */
	UserAccount saveAndFlush(UserAccount account);

	Optional<UserAccount> findByUsernameIgnoreCase(String username);

	boolean existsByUsernameIgnoreCase(String username);

}
