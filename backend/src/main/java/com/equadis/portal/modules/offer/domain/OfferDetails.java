package com.equadis.portal.modules.offer.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/**
 * Everything a supplier fills in for an offer. Validated on creation, so an Offer never holds
 * invalid details. BR-09: valid to is on or after valid from.
 */
public record OfferDetails(String reference, String title, String description, Discount discount,
		Integer minimumOrderQuantity, LocalDate validFrom, LocalDate validTo) {

	public static final int MAX_REFERENCE_LENGTH = 40;

	public static final int MAX_TITLE_LENGTH = 120;

	public static final int MAX_DESCRIPTION_LENGTH = 2000;

	/** Letters, digits, ".", "_" and "-", e.g. ALP-WINTER-01. */
	public static final String REFERENCE_REGEX = "^[A-Za-z0-9._-]{1,40}$";

	private static final Pattern REFERENCE = Pattern.compile(REFERENCE_REGEX);

	public OfferDetails {
		reference = reference == null ? "" : reference.trim();
		title = title == null ? "" : title.trim();
		description = description == null || description.isBlank() ? null : description.trim();
		if (!REFERENCE.matcher(reference).matches()) {
			throw invalid("Reference: 1 to " + MAX_REFERENCE_LENGTH + " letters, digits, '.', '_' or '-'");
		}
		if (title.isEmpty() || title.length() > MAX_TITLE_LENGTH) {
			throw invalid("Title is required (max " + MAX_TITLE_LENGTH + " characters)");
		}
		if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
			throw invalid("Description: max " + MAX_DESCRIPTION_LENGTH + " characters");
		}
		Objects.requireNonNull(discount, "discount");
		if (minimumOrderQuantity != null && minimumOrderQuantity < 1) {
			throw invalid("The minimum order quantity must be at least 1");
		}
		if (validFrom == null || validTo == null) {
			throw invalid("The validity window needs both dates");
		}
		if (validTo.isBefore(validFrom)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "Valid to must be on or after valid from");
		}
	}

	private static BusinessException invalid(String message) {
		return new BusinessException(ErrorCode.INVALID_OFFER, message);
	}

}
