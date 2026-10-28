package com.equadis.portal.modules.partnership.domain;

import java.time.LocalDate;
import java.util.Optional;

/** Persistence port for the Partnership aggregate. Implemented by Spring Data in infrastructure. */
public interface PartnershipRepository {

	/** Saves and flushes, so database constraints (including the overlap exclusion) are checked immediately. */
	Partnership saveAndFlush(Partnership partnership);

	Optional<Partnership> findById(Long id);

	/**
	 * True when the pair has another non-terminated partnership sharing at least one day with
	 * [start, end]. {@code excludedId} is the partnership being renewed (0 when creating).
	 */
	boolean existsOverlapping(long supplierId, long retailerId, LocalDate start, LocalDate end, long excludedId);

}
