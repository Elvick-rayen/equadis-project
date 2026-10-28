package com.equadis.portal.modules.offer;

/** Published when a supplier's offer is edited. */
public record OfferEdited(long offerId, long supplierId, String reference) {

}
