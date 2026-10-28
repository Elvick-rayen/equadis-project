package com.equadis.portal.modules.offer;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Public view of an offer for other modules (e.g. a retailer's subscriptions list). */
public record OfferSummary(long id, long supplierId, String reference, String title, DiscountType discountType,
		BigDecimal discountValue, String currency, LocalDate validFrom, LocalDate validTo, OfferStatus status) {

}
