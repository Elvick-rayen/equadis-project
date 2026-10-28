package com.equadis.portal.modules.offer.web;

import java.time.LocalDate;
import java.util.Set;

import com.equadis.portal.modules.offer.domain.Discount;
import com.equadis.portal.modules.offer.domain.Offer;
import com.equadis.portal.modules.offer.domain.OfferAction;
import com.equadis.portal.modules.offer.domain.OfferDetails;

/** Maps between HTTP DTOs and the domain. Building the Discount here is where BR-09 gets checked. */
final class OfferWebMapper {

	private OfferWebMapper() {
	}

	static OfferDetails toDetails(OfferRequest request) {
		return new OfferDetails(request.reference(), request.title(), request.description(),
				Discount.of(request.discountType(), request.discountValue(), request.currency()),
				request.minimumOrderQuantity(), request.validFrom(), request.validTo());
	}

	static OfferResponse toResponse(Offer offer, LocalDate today) {
		return toResponse(offer, null, offer.allowedActions(today));
	}

	/** For retailers: no lifecycle actions, but the supplier's name. */
	static OfferResponse toAvailable(Offer offer, String supplierName) {
		return toResponse(offer, supplierName, Set.of());
	}

	private static OfferResponse toResponse(Offer offer, String supplierName, Set<OfferAction> actions) {
		Discount discount = offer.discount();
		return new OfferResponse(offer.id(), offer.supplierId(), supplierName, offer.reference(), offer.title(),
				offer.description(), discount.type(), discount.value(),
				discount.currency() == null ? null : discount.currency().getCurrencyCode(), offer.minimumOrderQuantity(),
				offer.validFrom(), offer.validTo(), offer.status(), actions);
	}

}
