package com.equadis.portal.modules.offer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Public API of the offer module. BR-10 is decided here, in one place. */
public interface OfferAvailability {

	/**
	 * BR-10: the offer is PUBLISHED, today is within its validity window, and the retailer has
	 * an ACTIVE partnership with the offer's supplier.
	 */
	boolean isAvailableTo(long offerId, long retailerId);

	Optional<OfferSummary> findSummary(long offerId);

	List<OfferSummary> findSummaries(Collection<Long> offerIds);

}
