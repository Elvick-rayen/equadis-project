package com.equadis.portal.modules.retailer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.equadis.portal.modules.retailer.StoreType;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.shared.domain.Email;
import com.equadis.portal.shared.domain.ErrorCode;

class RetailerTest {

	private static final Location LYON = Location.forStoreType(StoreType.PHYSICAL, CountryCode.of("FR"), "Lyon",
			"12 Rue de la République", null);

	private static final Location ONLINE_BE = Location.forStoreType(StoreType.ONLINE, CountryCode.of("BE"), null, null,
			"https://shop.example.be");

	@Test
	void registerTrimsTheStoreName() {
		Retailer retailer = Retailer.register("  City Market Lyon  ", StoreType.PHYSICAL, LYON, Email.of("a@b.fr"));

		assertThat(retailer.storeName()).isEqualTo("City Market Lyon");
		assertThat(retailer.storeType()).isEqualTo(StoreType.PHYSICAL);
		assertThat(retailer.location()).isEqualTo(LYON);
		assertThat(retailer.contactEmail()).isEqualTo(Email.of("a@b.fr"));
	}

	@Test
	void updateReplacesAllDetails() {
		Retailer retailer = Retailer.register("City Market Lyon", StoreType.PHYSICAL, LYON, Email.of("a@b.fr"));

		retailer.update("City Market Online", StoreType.ONLINE, ONLINE_BE, Email.of("shop@example.be"));

		assertThat(retailer.storeName()).isEqualTo("City Market Online");
		assertThat(retailer.storeType()).isEqualTo(StoreType.ONLINE);
		assertThat(retailer.location().website()).isEqualTo("https://shop.example.be");
		assertThat(retailer.location().city()).isNull();
		assertThat(retailer.contactEmail().value()).isEqualTo("shop@example.be");
	}

	@Test
	void updateUsesTheSameRulesAsRegister() {
		Retailer retailer = Retailer.register("City Market Lyon", StoreType.PHYSICAL, LYON, Email.of("a@b.fr"));

		assertThatThrownBy(() -> retailer.update("   ", StoreType.PHYSICAL, LYON, Email.of("a@b.fr")))
			.isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_RETAILER));
		// A failed update leaves the retailer unchanged.
		assertThat(retailer.storeName()).isEqualTo("City Market Lyon");
	}

}
