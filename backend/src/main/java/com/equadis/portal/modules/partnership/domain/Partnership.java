package com.equadis.portal.modules.partnership.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.equadis.portal.modules.partnership.PartnershipStatus;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.DateRange;
import com.equadis.portal.shared.domain.ErrorCode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A partnership links one supplier and one retailer, by id, for a period. Aggregate root
 * enforcing BR-03, BR-05, BR-06 and BR-07; BR-04 (no overlap) needs the other partnerships
 * and is checked by {@link OverlapPolicy}.
 *
 * <p>Every rule that depends on the date takes {@code today} as a parameter, so the class
 * never reads the clock itself and tests can use any day.
 */
@Entity
@Table(name = "partnership")
public class Partnership {

	/** BR-03 */
	public static final int MAX_DURATION_MONTHS = 36;

	/** BR-06: an expired partnership can still be renewed for this many days. */
	public static final int RENEWAL_GRACE_DAYS = 30;

	public static final int MAX_REASON_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "supplier_id", nullable = false)
	private long supplierId;

	@Column(name = "retailer_id", nullable = false)
	private long retailerId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "terminated_on")
	private LocalDate terminatedOn;

	@Column(name = "termination_reason", length = MAX_REASON_LENGTH)
	private String terminationReason;

	@OneToMany(mappedBy = "partnership", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("renewedOn ASC, id ASC")
	private List<Renewal> renewals = new ArrayList<>();

	@Version
	private long version;

	protected Partnership() {
	}

	private Partnership(long supplierId, long retailerId, DateRange period) {
		this.supplierId = supplierId;
		this.retailerId = retailerId;
		this.startDate = period.start();
		this.endDate = period.end();
	}

	/** BR-03: the period ends after it starts (DateRange), does not start in the past and lasts at most 36 months. */
	public static Partnership create(long supplierId, long retailerId, DateRange period, LocalDate today) {
		Objects.requireNonNull(period, "period");
		if (period.start().isBefore(today)) {
			throw new BusinessException(ErrorCode.START_DATE_IN_PAST, "A partnership cannot start in the past");
		}
		requireMaxDuration(period.start(), period.end());
		return new Partnership(supplierId, retailerId, period);
	}

	/** BR-05: computed from the dates, so an ended partnership is EXPIRED without any action. */
	public PartnershipStatus status(LocalDate today) {
		if (terminatedOn != null) {
			return PartnershipStatus.TERMINATED;
		}
		if (today.isBefore(startDate)) {
			return PartnershipStatus.SCHEDULED;
		}
		if (today.isAfter(endDate)) {
			return PartnershipStatus.EXPIRED;
		}
		return PartnershipStatus.ACTIVE;
	}

	public Set<PartnershipAction> allowedActions(LocalDate today) {
		Set<PartnershipAction> actions = EnumSet.noneOf(PartnershipAction.class);
		if (canRenew(today)) {
			actions.add(PartnershipAction.RENEW);
		}
		if (canTerminate(today)) {
			actions.add(PartnershipAction.TERMINATE);
		}
		return actions;
	}

	/** BR-06: ACTIVE, or EXPIRED for at most 30 days. */
	public boolean canRenew(LocalDate today) {
		PartnershipStatus status = status(today);
		return status == PartnershipStatus.ACTIVE
				|| status == PartnershipStatus.EXPIRED && !today.isAfter(endDate.plusDays(RENEWAL_GRACE_DAYS));
	}

	/** BR-07: SCHEDULED or ACTIVE. */
	public boolean canTerminate(LocalDate today) {
		PartnershipStatus status = status(today);
		return status == PartnershipStatus.SCHEDULED || status == PartnershipStatus.ACTIVE;
	}

	/**
	 * BR-06: moves the end date and records the renewal. The new end must be after the current
	 * end, and the renewed period (from today, or from the current end if later) lasts at most
	 * 36 months. The caller checks the new period against other partnerships (BR-04).
	 */
	public Renewal renew(LocalDate newEndDate, LocalDate today) {
		checkRenewal(newEndDate, today);
		Renewal renewal = new Renewal(this, endDate, newEndDate, today);
		renewals.add(renewal);
		endDate = newEndDate;
		return renewal;
	}

	/**
	 * The BR-06 checks of {@link #renew}, without changing anything. Lets the caller check the
	 * new period against other partnerships (BR-04) before this one is modified.
	 */
	public void checkRenewal(LocalDate newEndDate, LocalDate today) {
		if (!canRenew(today)) {
			throw new BusinessException(ErrorCode.RENEWAL_NOT_ALLOWED,
					"Only an ACTIVE partnership, or one EXPIRED for at most " + RENEWAL_GRACE_DAYS + " days, can be renewed");
		}
		if (newEndDate == null || !newEndDate.isAfter(endDate)) {
			throw new BusinessException(ErrorCode.INVALID_RENEWAL_DATE, "The new end date must be after " + endDate);
		}
		LocalDate renewedFrom = today.isAfter(endDate) ? today : endDate;
		requireMaxDuration(renewedFrom, newEndDate);
	}

	/** BR-07: only SCHEDULED or ACTIVE, and the reason is mandatory. */
	public void terminate(String reason, LocalDate today) {
		if (!canTerminate(today)) {
			throw new BusinessException(ErrorCode.TERMINATION_NOT_ALLOWED,
					"Only a SCHEDULED or ACTIVE partnership can be terminated");
		}
		String cleanReason = reason == null ? "" : reason.trim();
		if (cleanReason.isEmpty() || cleanReason.length() > MAX_REASON_LENGTH) {
			throw new BusinessException(ErrorCode.TERMINATION_REASON_REQUIRED,
					"A termination reason is required (max " + MAX_REASON_LENGTH + " characters)");
		}
		this.terminatedOn = today;
		this.terminationReason = cleanReason;
	}

	private static void requireMaxDuration(LocalDate from, LocalDate to) {
		if (to.isAfter(from.plusMonths(MAX_DURATION_MONTHS))) {
			throw new BusinessException(ErrorCode.PARTNERSHIP_TOO_LONG,
					"A partnership lasts at most " + MAX_DURATION_MONTHS + " months");
		}
	}

	public Long id() {
		return id;
	}

	public long supplierId() {
		return supplierId;
	}

	public long retailerId() {
		return retailerId;
	}

	public LocalDate startDate() {
		return startDate;
	}

	public LocalDate endDate() {
		return endDate;
	}

	public DateRange period() {
		return DateRange.of(startDate, endDate);
	}

	public LocalDate terminatedOn() {
		return terminatedOn;
	}

	public String terminationReason() {
		return terminationReason;
	}

	public List<Renewal> renewals() {
		return Collections.unmodifiableList(renewals);
	}

}
