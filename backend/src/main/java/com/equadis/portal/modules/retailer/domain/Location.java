package com.equadis.portal.modules.retailer.domain;

import com.equadis.portal.modules.retailer.StoreType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.shared.domain.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Where a retailer sells. Create it with {@link #forStoreType}, which enforces BR-02:
 * ONLINE needs a website, PHYSICAL needs address and city, HYBRID needs all three.
 * Fields a store type does not use are dropped.
 */
@Embeddable
public record Location(
		@Column(name = "country_code", nullable = false, length = 2) String countryCode,
		@Column(name = "city", length = 120) String city,
		@Column(name = "address", length = 200) String address,
		@Column(name = "website", length = 255) String website) {

	public static Location forStoreType(StoreType storeType, CountryCode country, String city, String address,
			String website) {
		Objects.requireNonNull(storeType, "storeType");
		Objects.requireNonNull(country, "country");
		String cleanCity = blankToNull(city);
		String cleanAddress = blankToNull(address);
		String cleanWebsite = blankToNull(website);

		if (storeType.needsStreetAddress() && (cleanCity == null || cleanAddress == null)) {
			throw invalid(storeType + " stores need a city and an address");
		}
		if (storeType.needsWebsite()) {
			if (cleanWebsite == null) {
				throw invalid(storeType + " stores need a website");
			}
			requireHttpUrl(cleanWebsite);
		}
		if (cleanCity != null && cleanCity.length() > 120 || cleanAddress != null && cleanAddress.length() > 200
				|| cleanWebsite != null && cleanWebsite.length() > 255) {
			throw invalid("Location field too long");
		}
		return new Location(country.value(), storeType.needsStreetAddress() ? cleanCity : null,
				storeType.needsStreetAddress() ? cleanAddress : null, storeType.needsWebsite() ? cleanWebsite : null);
	}

	private static void requireHttpUrl(String website) {
		try {
			URI uri = new URI(website);
			if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
					|| uri.getHost() == null) {
				throw invalid("Website must be an http(s) URL");
			}
		}
		catch (URISyntaxException ex) {
			throw invalid("Website must be an http(s) URL");
		}
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static BusinessException invalid(String message) {
		return new BusinessException(ErrorCode.INVALID_LOCATION, message);
	}

}
