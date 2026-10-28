package com.equadis.portal.modules.offer.domain;

import java.util.Optional;

/** Persistence port for the Offer aggregate. Implemented by Spring Data in infrastructure. */
public interface OfferRepository {

	/** Saves and flushes, so database constraints are checked immediately. */
	Offer saveAndFlush(Offer offer);

	Optional<Offer> findById(Long id);

	/** References are unique per supplier, ignoring case. {@code excludedId}: the offer being edited, or 0. */
	boolean existsReference(long supplierId, String reference, long excludedId);

}
