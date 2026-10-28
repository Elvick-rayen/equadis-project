package com.equadis.portal.modules.partnership;

import java.time.LocalDate;

/** Published when a partnership is created. */
public record PartnershipCreated(long partnershipId, long supplierId, long retailerId, LocalDate startDate,
		LocalDate endDate) {

}
