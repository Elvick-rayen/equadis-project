package com.equadis.portal.modules.partnership;

import java.time.LocalDate;

/** Published when a partnership gets a new end date (BR-06). */
public record PartnershipRenewed(long partnershipId, long supplierId, long retailerId, LocalDate previousEndDate,
		LocalDate newEndDate) {

}
