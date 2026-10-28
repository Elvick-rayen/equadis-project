package com.equadis.portal.modules.offer;

/** Published when a supplier's offer is published. */
public record OfferPublished(long offerId, long supplierId, String reference) {

}
