package com.equadis.portal.modules.partnership.web;

import java.time.LocalDate;

import com.equadis.portal.modules.partnership.application.command.CreatePartnership;
import com.equadis.portal.modules.partnership.domain.Partnership;
import com.equadis.portal.modules.partnership.infrastructure.PartnershipReadModel.PartyNames;
import com.equadis.portal.shared.domain.DateRange;

/** Maps between HTTP DTOs and the domain. */
final class PartnershipWebMapper {

	private PartnershipWebMapper() {
	}

	static CreatePartnership toCreate(PartnershipRequests.Create request) {
		return new CreatePartnership(request.supplierId(), request.retailerId(),
				DateRange.of(request.startDate(), request.endDate()));
	}

	/** For lists: no renewal history, to avoid loading it for every row. */
	static PartnershipResponse toSummary(Partnership partnership, PartyNames names, LocalDate today) {
		return toResponse(partnership, names, today, false);
	}

	static PartnershipResponse toDetail(Partnership partnership, PartyNames names, LocalDate today) {
		return toResponse(partnership, names, today, true);
	}

	private static PartnershipResponse toResponse(Partnership partnership, PartyNames names, LocalDate today,
			boolean withRenewals) {
		return new PartnershipResponse(partnership.id(), partnership.supplierId(), names.supplier(partnership.supplierId()),
				partnership.retailerId(), names.retailer(partnership.retailerId()), partnership.startDate(),
				partnership.endDate(), partnership.status(today), partnership.allowedActions(today),
				partnership.terminatedOn(), partnership.terminationReason(),
				withRenewals ? partnership.renewals()
					.stream()
					.map(r -> new PartnershipResponse.RenewalResponse(r.previousEndDate(), r.newEndDate(), r.renewedOn()))
					.toList() : null);
	}

}
