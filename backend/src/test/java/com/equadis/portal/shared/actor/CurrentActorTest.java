package com.equadis.portal.shared.actor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

class CurrentActorTest {

	@Test
	void parsesEachPersonaKey() {
		assertThat(CurrentActor.parse("ADMIN")).isEqualTo(CurrentActor.admin());
		assertThat(CurrentActor.parse("SUPPLIER:12")).isEqualTo(CurrentActor.supplier(12));
		assertThat(CurrentActor.parse(" RETAILER:3 ")).isEqualTo(CurrentActor.retailer(3));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "BOSS", "admin", "ADMIN:1", "SUPPLIER", "SUPPLIER:", "SUPPLIER:abc", "RETAILER:1:2" })
	void malformedPersonaKeyIsRejected(String key) {
		assertThatThrownBy(() -> CurrentActor.parse(key))
			.isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_ACTING_AS));
	}

	@Test
	void personaOnlyMatchesItsOwnCompany() {
		CurrentActor supplier = CurrentActor.supplier(12);
		assertThat(supplier.isSupplier(12)).isTrue();
		assertThat(supplier.isSupplier(13)).isFalse();
		assertThat(supplier.isRetailer(12)).isFalse();
		assertThat(supplier.isAdmin()).isFalse();
	}

}
