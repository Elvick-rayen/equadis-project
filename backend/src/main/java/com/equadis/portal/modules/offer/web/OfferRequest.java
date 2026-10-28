package com.equadis.portal.modules.offer.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.equadis.portal.modules.offer.DiscountType;
import com.equadis.portal.modules.offer.domain.OfferDetails;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST and PUT /api/suppliers/{supplierId}/offers. Checks the shape of the input; the
 * discount rules (BR-09) and the validity window are checked by the domain.
 */
public record OfferRequest(
		@NotBlank @Pattern(regexp = OfferDetails.REFERENCE_REGEX,
				message = "1 to 40 letters, digits, '.', '_' or '-'") String reference,
		@NotBlank @Size(max = OfferDetails.MAX_TITLE_LENGTH) String title,
		@Size(max = OfferDetails.MAX_DESCRIPTION_LENGTH) String description,
		@NotNull DiscountType discountType,
		@NotNull @Digits(integer = 10, fraction = 2) BigDecimal discountValue,
		@Pattern(regexp = "^[A-Za-z]{3}$", message = "must be an ISO 4217 code, e.g. EUR") String currency,
		@Min(1) Integer minimumOrderQuantity,
		@NotNull LocalDate validFrom,
		@NotNull LocalDate validTo) {

}
