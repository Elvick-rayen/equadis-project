package com.equadis.portal.modules.retailer.domain;

import com.equadis.portal.modules.retailer.StoreType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.shared.domain.ErrorCode;

/** BR-02: required location data depends on the store type. */
class LocationTest {

	private static final CountryCode FR = CountryCode.of("FR");

	private static final String CITY = "Lyon";

	private static final String ADDRESS = "12 Rue de la République";

	private static final String WEBSITE = "https://shop.example.fr";

	@ParameterizedTest(name = "{0} with city={1}, address={2}, website={3} -> valid={4}")
	@CsvSource(nullValues = "-", value = {
			// type,     city, address, website, valid
			"ONLINE,     -,    -,       site,    true",
			"ONLINE,     city, addr,    -,       false",
			"PHYSICAL,   city, addr,    -,       true",
			"PHYSICAL,   city, -,       site,    false",
			"PHYSICAL,   -,    addr,    -,       false",
			"HYBRID,     city, addr,    site,    true",
			"HYBRID,     city, addr,    -,       false",
			"HYBRID,     -,    -,       site,    false" })
	void requiredFieldsDependOnStoreType(StoreType type, String city, String address, String website, boolean valid) {
		String c = city == null ? null : CITY;
		String a = address == null ? null : ADDRESS;
		String w = website == null ? null : WEBSITE;
		if (valid) {
			assertThat(Location.forStoreType(type, FR, c, a, w)).isNotNull();
		}
		else {
			assertThatThrownBy(() -> Location.forStoreType(type, FR, c, a, w)).isInstanceOfSatisfying(
					BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_LOCATION));
		}
	}

	@Test
	void fieldsTheStoreTypeDoesNotUseAreDropped() {
		Location online = Location.forStoreType(StoreType.ONLINE, FR, CITY, ADDRESS, WEBSITE);
		assertThat(online.city()).isNull();
		assertThat(online.address()).isNull();
		assertThat(online.website()).isEqualTo(WEBSITE);

		Location physical = Location.forStoreType(StoreType.PHYSICAL, FR, CITY, ADDRESS, WEBSITE);
		assertThat(physical.website()).isNull();
	}

	@Test
	void blankCountsAsMissingAndWebsiteMustBeHttp() {
		assertThatThrownBy(() -> Location.forStoreType(StoreType.PHYSICAL, FR, "  ", ADDRESS, null))
			.isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> Location.forStoreType(StoreType.ONLINE, FR, null, null, "ftp://shop.example.fr"))
			.isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> Location.forStoreType(StoreType.ONLINE, FR, null, null, "not a url"))
			.isInstanceOf(BusinessException.class);
	}

}
