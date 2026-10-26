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
	void parsesEachPersona() {
		assertThat(CurrentActor.parse("ADMIN")).isEqualTo(CurrentActor.admin());
		assertThat(CurrentActor.parse("SUPPLIER:12")).isEqualTo(CurrentActor.supplier(12));
		assertThat(CurrentActor.parse(" RETAILER:3 ")).isEqualTo(CurrentActor.retailer(3));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void missingHeaderIsReported(String header) {
		assertThatThrownBy(() -> CurrentActor.parse(header))
			.isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.ACTING_AS_REQUIRED));
	}

	@ParameterizedTest
	@ValueSource(strings = { "BOSS", "admin", "ADMIN:1", "SUPPLIER", "SUPPLIER:", "SUPPLIER:abc", "RETAILER:1:2" })
	void malformedHeaderIsRejected(String header) {
		assertThatThrownBy(() -> CurrentActor.parse(header))
			.isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_ACTING_AS));
	}

	@Test
	void supplierOnlyMatchesItsOwnId() {
		CurrentActor actor = CurrentActor.supplier(12);

		assertThat(actor.isSupplier(12)).isTrue();
		assertThat(actor.isSupplier(13)).isFalse();
		assertThat(actor.isAdmin()).isFalse();
	}

}
