package com.equadis.portal.modules.offer.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.modules.offer.infrastructure.OfferReadModel;
import com.equadis.portal.shared.actor.ActorAccess;
import com.equadis.portal.shared.actor.CurrentActor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Offers")
class AvailableOfferController {

	private final OfferReadModel readModel;

	private final ActorAccess access;

	AvailableOfferController(OfferReadModel readModel, ActorAccess access) {
		this.readModel = readModel;
		this.access = access;
	}

	@GetMapping("/api/retailers/{retailerId}/available-offers")
	@Operation(summary = "Offers a retailer can see and subscribe to today - BR-10 (administrator or that retailer)",
			description = "Published, within their validity window, from suppliers with an ACTIVE partnership with the retailer.")
	List<OfferResponse> available(CurrentActor actor, @PathVariable long retailerId) {
		access.requireAdminOrRetailer(actor, retailerId);
		return readModel.availableTo(retailerId)
			.stream()
			.map(available -> OfferWebMapper.toAvailable(available.offer(), available.supplierName()))
			.toList();
	}

}
