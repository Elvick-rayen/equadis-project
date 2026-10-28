package com.equadis.portal.modules.partnership;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Public API of the partnership module: what other modules may ask about partnerships, as of today. */
public interface PartnershipQueries {

	/** True when the pair has an ACTIVE partnership today (used by BR-10 and BR-12). */
	boolean hasActivePartnership(long supplierId, long retailerId);

	/** Suppliers that have an ACTIVE partnership with this retailer today. */
	Set<Long> activeSupplierIds(long retailerId);

	/** Non-terminated partnerships whose end date is exactly this day (expiry notifications). */
	List<ExpiringPartnership> endingOn(LocalDate endDate);

	record ExpiringPartnership(long partnershipId, long supplierId, long retailerId, LocalDate endDate) {
	}

}
