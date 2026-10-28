package com.equadis.portal.modules.notification.domain;

import java.time.LocalDate;

/** A partnership that ends in {@code daysLeft} days. */
public record ExpiryNotice(long partnershipId, String supplierName, String retailerName, LocalDate endDate,
		int daysLeft) {

	public String message() {
		return "Partnership #%d between %s and %s ends in %d day%s (%s)".formatted(partnershipId, supplierName,
				retailerName, daysLeft, daysLeft == 1 ? "" : "s", endDate);
	}

}
