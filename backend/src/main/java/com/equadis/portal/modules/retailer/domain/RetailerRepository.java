package com.equadis.portal.modules.retailer.domain;

import java.util.Optional;

/** Persistence port for the Retailer aggregate. Implemented by Spring Data in infrastructure. */
public interface RetailerRepository {

	/** Saves and flushes, so database constraints are checked immediately. */
	Retailer saveAndFlush(Retailer retailer);

	Optional<Retailer> findById(Long id);

	void delete(Retailer retailer);
}
