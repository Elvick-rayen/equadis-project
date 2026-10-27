package com.equadis.portal.retailer;

import java.util.List;
import java.util.Optional;

/** Public API of the retailer module: what other modules may ask about retailers. */
public interface RetailerQueries {

	Optional<RetailerSummary> findSummary(long retailerId);

	/** All retailers, sorted by store name. */
	List<RetailerSummary> findAllSummaries();

	Optional<RetailerSummary> findByStoreName(String storeName);

}
