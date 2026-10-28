package com.equadis.portal.modules.offer;

/** Published when a supplier's offer is created. */
public record OfferCreated(long offerId, long supplierId, String reference) {

}
