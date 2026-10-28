package com.equadis.portal.modules.subscription.domain;

import java.time.LocalDate;
import java.util.Objects;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A retailer subscribed to an offer (both by id). Cancelled with a date and a cause, never
 * deleted, so the history stays visible. Whether a retailer may subscribe (BR-10, BR-11) needs
 * other data and is checked by the application service.
 */
@Entity
@Table(name = "subscription")
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "retailer_id", nullable = false)
	private long retailerId;

	@Column(name = "offer_id", nullable = false)
	private long offerId;

	@Column(name = "supplier_id", nullable = false)
	private long supplierId;

	@Column(name = "subscribed_on", nullable = false)
	private LocalDate subscribedOn;

	@Column(name = "cancelled_on")
	private LocalDate cancelledOn;

	@Enumerated(EnumType.STRING)
	@Column(name = "cancellation_cause", length = 30)
	private CancellationCause cancellationCause;

	@Version
	private long version;

	protected Subscription() {
	}

	private Subscription(long retailerId, long offerId, long supplierId, LocalDate subscribedOn) {
		this.retailerId = retailerId;
		this.offerId = offerId;
		this.supplierId = supplierId;
		this.subscribedOn = Objects.requireNonNull(subscribedOn, "subscribedOn");
	}

	public static Subscription subscribe(long retailerId, long offerId, long supplierId, LocalDate today) {
		return new Subscription(retailerId, offerId, supplierId, today);
	}

	public void cancel(CancellationCause cause, LocalDate today) {
		if (isCancelled()) {
			throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED,
					"Subscription " + id + " was already cancelled on " + cancelledOn);
		}
		this.cancellationCause = Objects.requireNonNull(cause, "cause");
		this.cancelledOn = today;
	}

	/**
	 * BR-12: a subscription that is not cancelled is SUSPENDED while the retailer has no ACTIVE
	 * partnership with the offer's supplier (e.g. it expired), and ACTIVE again once renewed.
	 */
	public SubscriptionState state(boolean partnershipActive) {
		if (isCancelled()) {
			return SubscriptionState.CANCELLED;
		}
		return partnershipActive ? SubscriptionState.ACTIVE : SubscriptionState.SUSPENDED;
	}

	public boolean isCancelled() {
		return cancelledOn != null;
	}

	public Long id() {
		return id;
	}

	public long retailerId() {
		return retailerId;
	}

	public long offerId() {
		return offerId;
	}

	public long supplierId() {
		return supplierId;
	}

	public LocalDate subscribedOn() {
		return subscribedOn;
	}

	public LocalDate cancelledOn() {
		return cancelledOn;
	}

	public CancellationCause cancellationCause() {
		return cancellationCause;
	}

}
