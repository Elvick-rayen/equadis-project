package com.equadis.portal.modules.subscription.domain;

import java.util.List;
import java.util.Optional;

/** Persistence port for subscriptions. Implemented by Spring Data in infrastructure. */
public interface SubscriptionRepository {

	/** Saves and flushes, so the BR-11 unique index is checked immediately. */
	Subscription saveAndFlush(Subscription subscription);

	Optional<Subscription> findById(Long id);

	/** BR-11 */
	boolean existsActive(long retailerId, long offerId);

	/** Not cancelled; a terminated partnership cancels these (BR-07). */
	List<Subscription> findActiveForRetailerAndSupplier(long retailerId, long supplierId);

	/** Not cancelled; a closed offer cancels these (BR-08). */
	List<Subscription> findActiveForOffer(long offerId);

}
