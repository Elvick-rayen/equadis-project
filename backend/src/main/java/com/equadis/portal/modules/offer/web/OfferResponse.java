package com.equadis.portal.modules.offer.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import com.equadis.portal.modules.offer.DiscountType;
import com.equadis.portal.modules.offer.OfferStatus;
import com.equadis.portal.modules.offer.domain.OfferAction;

/**
 * An offer as its supplier sees it, with the actions allowed now (BR-08).
 * {@code supplierName} is filled for offers shown to retailers.
 */
public record OfferResponse(Long id, long supplierId, String supplierName, String reference, String title,
		String description, DiscountType discountType, BigDecimal discountValue, String currency,
		Integer minimumOrderQuantity, LocalDate validFrom, LocalDate validTo, OfferStatus status,
		Set<OfferAction> allowedActions) {

}
