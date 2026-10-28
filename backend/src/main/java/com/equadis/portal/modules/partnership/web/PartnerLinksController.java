package com.equadis.portal.modules.partnership.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.modules.partnership.infrastructure.PartnershipReadModel;
import com.equadis.portal.modules.partnership.infrastructure.PartnershipReadModel.LinkedPartner;
import com.equadis.portal.shared.actor.ActorAccess;
import com.equadis.portal.shared.actor.CurrentActor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Which companies are linked, i.e. have an ACTIVE partnership today. Lives in the partnership
 * module because partnerships are what link them; the paths read naturally from either side.
 */
@RestController
@Tag(name = "Partnerships")
class PartnerLinksController {

	record LinkedPartnerResponse(long partnershipId, long id, String name, LocalDate startDate, LocalDate endDate) {
	}

	private final PartnershipReadModel readModel;

	private final ActorAccess access;

	PartnerLinksController(PartnershipReadModel readModel, ActorAccess access) {
		this.readModel = readModel;
		this.access = access;
	}

	@GetMapping("/api/suppliers/{supplierId}/retailers")
	@Operation(summary = "Retailers linked to a supplier (administrator or that supplier)")
	List<LinkedPartnerResponse> retailersOfSupplier(CurrentActor actor, @PathVariable long supplierId) {
		access.requireAdminOrSupplier(actor, supplierId);
		return toResponse(readModel.linkedRetailers(supplierId));
	}

	@GetMapping("/api/retailers/{retailerId}/suppliers")
	@Operation(summary = "Suppliers linked to a retailer (administrator or that retailer)")
	List<LinkedPartnerResponse> suppliersOfRetailer(CurrentActor actor, @PathVariable long retailerId) {
		access.requireAdminOrRetailer(actor, retailerId);
		return toResponse(readModel.linkedSuppliers(retailerId));
	}

	private static List<LinkedPartnerResponse> toResponse(List<LinkedPartner> partners) {
		return partners.stream()
			.map(p -> new LinkedPartnerResponse(p.partnershipId(), p.companyId(), p.companyName(), p.startDate(),
					p.endDate()))
			.toList();
	}

}
