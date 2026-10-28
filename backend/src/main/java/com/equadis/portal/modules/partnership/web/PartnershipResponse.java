package com.equadis.portal.modules.partnership.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.equadis.portal.modules.partnership.PartnershipStatus;
import com.equadis.portal.modules.partnership.domain.PartnershipAction;

/**
 * A partnership as of today: computed status (BR-05) and the actions allowed now, so the UI
 * shows Renew / Terminate only when they would succeed. {@code renewals} is filled on the
 * detail endpoint and null in lists.
 */
public record PartnershipResponse(Long id, long supplierId, String supplierName, long retailerId, String retailerName,
		LocalDate startDate, LocalDate endDate, PartnershipStatus status, Set<PartnershipAction> allowedActions,
		LocalDate terminatedOn, String terminationReason, List<RenewalResponse> renewals) {

	public record RenewalResponse(LocalDate previousEndDate, LocalDate newEndDate, LocalDate renewedOn) {
	}

}
