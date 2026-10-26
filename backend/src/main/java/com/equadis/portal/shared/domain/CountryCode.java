package com.equadis.portal.shared.domain;

import java.util.Locale;
import java.util.Set;

/** ISO 3166-1 alpha-2 country code, e.g. {@code FR}. Input is trimmed and upper-cased. */
public record CountryCode(String value) {

	private static final Set<String> ISO_CODES = Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2);

	public CountryCode {
		value = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
		if (!ISO_CODES.contains(value)) {
			throw new BusinessException(ErrorCode.INVALID_COUNTRY_CODE, "Unknown ISO 3166-1 alpha-2 country code: '" + value + "'");
		}
	}

	public static CountryCode of(String value) {
		return new CountryCode(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
