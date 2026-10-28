package com.equadis.portal.modules.subscription.domain;

/** Why a subscription was cancelled. */
public enum CancellationCause {

	/** The retailer unsubscribed. */
	RETAILER_REQUEST,

	/** BR-08: the supplier closed the offer. */
	OFFER_CLOSED,

	/** BR-07: the partnership between the retailer and the offer's supplier was terminated. */
	PARTNERSHIP_TERMINATED

}
