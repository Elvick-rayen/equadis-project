package com.equadis.portal.modules.subscription.application;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.offer.OfferAvailability;
import com.equadis.portal.modules.offer.OfferSummary;
import com.equadis.portal.modules.subscription.domain.CancellationCause;
import com.equadis.portal.modules.subscription.domain.Subscription;
import com.equadis.portal.modules.subscription.domain.SubscriptionRepository;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/** Subscribe and unsubscribe. BR-10 is asked of the offer module; BR-11 is checked here and by the database. */
@Service
@Transactional
public class SubscriptionService {

	private final SubscriptionRepository subscriptions;

	private final OfferAvailability offers;

	private final Clock clock;

	public SubscriptionService(SubscriptionRepository subscriptions, OfferAvailability offers, Clock clock) {
		this.subscriptions = subscriptions;
		this.offers = offers;
		this.clock = clock;
	}

	public Subscription subscribe(long retailerId, long offerId) {
		OfferSummary offer = offers.findSummary(offerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.OFFER_NOT_FOUND, "Offer " + offerId + " not found"));
		if (!offers.isAvailableTo(offerId, retailerId)) {
			throw new BusinessException(ErrorCode.OFFER_NOT_AVAILABLE,
					"This offer is not available: it must be published, valid today, and from an active partner");
		}
		if (subscriptions.existsActive(retailerId, offerId)) {
			throw alreadySubscribed();
		}
		try {
			return subscriptions.saveAndFlush(Subscription.subscribe(retailerId, offerId, offer.supplierId(), today()));
		}
		catch (DataIntegrityViolationException ex) {
			throw alreadySubscribed();
		}
	}

	public Subscription unsubscribe(long retailerId, long subscriptionId) {
		Subscription subscription = subscriptions.findById(subscriptionId)
			.filter(s -> s.retailerId() == retailerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND,
					"Subscription " + subscriptionId + " not found"));
		subscription.cancel(CancellationCause.RETAILER_REQUEST, today());
		return subscriptions.saveAndFlush(subscription);
	}

	public LocalDate today() {
		return LocalDate.now(clock);
	}

	private static BusinessException alreadySubscribed() {
		return new BusinessException(ErrorCode.ALREADY_SUBSCRIBED, "You already have an active subscription to this offer");
	}

}
