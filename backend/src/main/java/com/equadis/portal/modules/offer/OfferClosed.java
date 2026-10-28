package com.equadis.portal.modules.offer;

import java.time.LocalDate;

/**
 * Published when an offer is closed (BR-08). Listeners run in the same transaction:
 * the subscription module cancels the offer's subscriptions.
 */
public record OfferClosed(long offerId, long supplierId, String reference, LocalDate closedOn) {

}
