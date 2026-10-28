package com.equadis.portal.modules.partnership;

import java.time.LocalDate;

/**
 * Published when a partnership is terminated (BR-07). Listeners run in the same transaction:
 * the subscription module cancels the retailer's active subscriptions to this supplier's offers.
 */
public record PartnershipTerminated(long partnershipId, long supplierId, long retailerId, String reason,
		LocalDate terminatedOn) {

}
