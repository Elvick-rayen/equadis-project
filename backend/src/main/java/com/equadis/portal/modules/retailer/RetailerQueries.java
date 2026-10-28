package com.equadis.portal.modules.retailer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Public API of the retailer module: what other modules may ask about retailers. */
public interface RetailerQueries {

	boolean exists(long retailerId);

	Optional<RetailerSummary> findSummary(long retailerId);

	/** All retailers, sorted by store name. */
	List<RetailerSummary> findAllSummaries();

	List<RetailerSummary> findSummaries(Collection<Long> retailerIds);

	Optional<RetailerSummary> findByStoreName(String storeName);

}
