package com.equadis.portal.modules.partnership.web;

import java.time.LocalDate;

import com.equadis.portal.modules.partnership.domain.Partnership;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request bodies of /api/partnerships. Dates are ISO calendar dates (yyyy-MM-dd). */
final class PartnershipRequests {

	private PartnershipRequests() {
	}

	record Create(@NotNull Long supplierId, @NotNull Long retailerId, @NotNull LocalDate startDate,
			@NotNull LocalDate endDate) {
	}

	record Renew(@NotNull LocalDate newEndDate) {
	}

	record Terminate(@NotBlank @Size(max = Partnership.MAX_REASON_LENGTH) String reason) {
	}

}
