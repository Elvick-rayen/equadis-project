package com.equadis.portal.modules.offer.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import com.equadis.portal.modules.offer.DiscountType;
import com.equadis.portal.modules.offer.OfferStatus;
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
 * A commercial offer published by one supplier (referenced by id). Aggregate root enforcing the
 * lifecycle (BR-08) and, through {@link OfferDetails} and {@link Discount}, BR-09.
 */
@Entity
@Table(name = "offer")
public class Offer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "supplier_id", nullable = false)
	private long supplierId;

	@Column(name = "reference", nullable = false, length = OfferDetails.MAX_REFERENCE_LENGTH)
	private String reference;

	@Column(name = "title", nullable = false, length = OfferDetails.MAX_TITLE_LENGTH)
	private String title;

	@Column(name = "description", length = OfferDetails.MAX_DESCRIPTION_LENGTH)
	private String description;

	/** The Discount is stored as three columns and rebuilt by {@link #discount()}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false, length = 30)
	private DiscountType discountType;

	@Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "currency", length = 3)
	private String currency;

	@Column(name = "minimum_order_quantity")
	private Integer minimumOrderQuantity;

	@Column(name = "valid_from", nullable = false)
	private LocalDate validFrom;

	@Column(name = "valid_to", nullable = false)
	private LocalDate validTo;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private OfferStatus status;

	@Version
	private long version;

	protected Offer() {
	}

	private Offer(long supplierId, OfferDetails details) {
		this.supplierId = supplierId;
		this.status = OfferStatus.DRAFT;
		apply(details);
	}

	/** Every offer starts as a draft. */
	public static Offer draft(long supplierId, OfferDetails details) {
		return new Offer(supplierId, Objects.requireNonNull(details, "details"));
	}

	/** BR-08: only drafts can be edited. */
	public void edit(OfferDetails details) {
		if (status != OfferStatus.DRAFT) {
			throw new BusinessException(ErrorCode.OFFER_NOT_EDITABLE, "Only a DRAFT offer can be edited (this one is " + status + ")");
		}
		apply(Objects.requireNonNull(details, "details"));
	}

	/** BR-08: DRAFT → PUBLISHED, while the validity window has not ended. */
	public void publish(LocalDate today) {
		if (status != OfferStatus.DRAFT) {
			throw transition("publish", OfferStatus.DRAFT);
		}
		if (validTo.isBefore(today)) {
			throw new BusinessException(ErrorCode.INVALID_OFFER_TRANSITION,
					"The validity window ended on " + validTo + "; edit the dates before publishing");
		}
		status = OfferStatus.PUBLISHED;
	}

	/** BR-08: PUBLISHED → CLOSED. The caller announces it so subscriptions get cancelled. */
	public void close() {
		if (status != OfferStatus.PUBLISHED) {
			throw transition("close", OfferStatus.PUBLISHED);
		}
		status = OfferStatus.CLOSED;
	}

	/** Part of BR-10: published and today within the validity window (the partnership is checked elsewhere). */
	public boolean isOpenOn(LocalDate today) {
		return status == OfferStatus.PUBLISHED && !today.isBefore(validFrom) && !today.isAfter(validTo);
	}

	public Set<OfferAction> allowedActions(LocalDate today) {
		return switch (status) {
			case DRAFT -> validTo.isBefore(today) ? EnumSet.of(OfferAction.EDIT) : EnumSet.of(OfferAction.EDIT, OfferAction.PUBLISH);
			case PUBLISHED -> EnumSet.of(OfferAction.CLOSE);
			case CLOSED -> EnumSet.noneOf(OfferAction.class);
		};
	}

	private void apply(OfferDetails details) {
		this.reference = details.reference();
		this.title = details.title();
		this.description = details.description();
		this.discountType = details.discount().type();
		this.discountValue = details.discount().value();
		this.currency = details.discount().currency() == null ? null : details.discount().currency().getCurrencyCode();
		this.minimumOrderQuantity = details.minimumOrderQuantity();
		this.validFrom = details.validFrom();
		this.validTo = details.validTo();
	}

	private BusinessException transition(String action, OfferStatus required) {
		return new BusinessException(ErrorCode.INVALID_OFFER_TRANSITION,
				"Cannot " + action + " an offer that is " + status + " (it must be " + required + ")");
	}

	public Discount discount() {
		return Discount.of(discountType, discountValue, currency);
	}

	public Long id() {
		return id;
	}

	public long supplierId() {
		return supplierId;
	}

	public String reference() {
		return reference;
	}

	public String title() {
		return title;
	}

	public String description() {
		return description;
	}

	public Integer minimumOrderQuantity() {
		return minimumOrderQuantity;
	}

	public LocalDate validFrom() {
		return validFrom;
	}

	public LocalDate validTo() {
		return validTo;
	}

	public OfferStatus status() {
		return status;
	}

}
