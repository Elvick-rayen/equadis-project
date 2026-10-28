package com.equadis.portal.modules.subscription.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.equadis.portal.modules.offer.OfferClosed;
import com.equadis.portal.modules.partnership.PartnershipTerminated;
import com.equadis.portal.modules.subscription.domain.CancellationCause;
import com.equadis.portal.modules.subscription.domain.Subscription;
import com.equadis.portal.modules.subscription.domain.SubscriptionRepository;

/**
 * Reacts to other modules' events. Synchronous on purpose: it runs inside the transaction that
 * terminates the partnership or closes the offer, so either both happen or neither does.
 * The partnership and offer modules never call this module, so there is no dependency cycle.
 */
@Component
class SubscriptionCancellationListener {

	private final SubscriptionRepository subscriptions;

	SubscriptionCancellationListener(SubscriptionRepository subscriptions) {
		this.subscriptions = subscriptions;
	}

	/** BR-07: termination cancels the retailer's active subscriptions to that supplier's offers. */
	@EventListener
	void on(PartnershipTerminated event) {
		cancel(subscriptions.findActiveForRetailerAndSupplier(event.retailerId(), event.supplierId()),
				CancellationCause.PARTNERSHIP_TERMINATED, event.terminatedOn());
	}

	/** BR-08: closing an offer cancels its subscriptions. */
	@EventListener
	void on(OfferClosed event) {
		cancel(subscriptions.findActiveForOffer(event.offerId()), CancellationCause.OFFER_CLOSED, event.closedOn());
	}

	private void cancel(List<Subscription> active, CancellationCause cause, LocalDate day) {
		// Loaded in the caller's transaction: the changes are saved when it commits.
		active.forEach(subscription -> subscription.cancel(cause, day));
	}

}
