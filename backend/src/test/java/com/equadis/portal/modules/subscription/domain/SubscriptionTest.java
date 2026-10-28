package com.equadis.portal.modules.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

class SubscriptionTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 12, 1);

	@Test
	void stateFollowsThePartnershipUntilCancelled() {
		Subscription subscription = Subscription.subscribe(1, 2, 3, TODAY);

		assertThat(subscription.state(true)).isEqualTo(SubscriptionState.ACTIVE);
		// BR-12: suspended while the partnership is not active, active again once it is.
		assertThat(subscription.state(false)).isEqualTo(SubscriptionState.SUSPENDED);
		assertThat(subscription.state(true)).isEqualTo(SubscriptionState.ACTIVE);

		subscription.cancel(CancellationCause.OFFER_CLOSED, TODAY.plusDays(3));
		assertThat(subscription.state(true)).isEqualTo(SubscriptionState.CANCELLED);
		assertThat(subscription.cancellationCause()).isEqualTo(CancellationCause.OFFER_CLOSED);
		assertThat(subscription.cancelledOn()).isEqualTo(TODAY.plusDays(3));
	}

	@Test
	void cannotBeCancelledTwice() {
		Subscription subscription = Subscription.subscribe(1, 2, 3, TODAY);
		subscription.cancel(CancellationCause.RETAILER_REQUEST, TODAY);

		assertThatThrownBy(() -> subscription.cancel(CancellationCause.OFFER_CLOSED, TODAY))
			.isInstanceOfSatisfying(BusinessException.class,
					ex -> assertThat(ex.code()).isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED));
	}

}
