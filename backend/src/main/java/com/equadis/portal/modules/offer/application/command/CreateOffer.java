package com.equadis.portal.modules.offer.application.command;

import com.equadis.portal.modules.offer.domain.OfferDetails;

public record CreateOffer(long supplierId, OfferDetails details) {

}
