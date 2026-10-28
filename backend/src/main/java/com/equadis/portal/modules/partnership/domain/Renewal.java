package com.equadis.portal.modules.partnership.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One entry of a partnership's renewal history (BR-06). Created only by {@link Partnership#renew}. */
@Entity
@Table(name = "partnership_renewal")
public class Renewal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "partnership_id", nullable = false)
	private Partnership partnership;

	@Column(name = "previous_end_date", nullable = false)
	private LocalDate previousEndDate;

	@Column(name = "new_end_date", nullable = false)
	private LocalDate newEndDate;

	@Column(name = "renewed_on", nullable = false)
	private LocalDate renewedOn;

	protected Renewal() {
	}

	Renewal(Partnership partnership, LocalDate previousEndDate, LocalDate newEndDate, LocalDate renewedOn) {
		this.partnership = partnership;
		this.previousEndDate = previousEndDate;
		this.newEndDate = newEndDate;
		this.renewedOn = renewedOn;
	}

	public Long id() {
		return id;
	}

	public LocalDate previousEndDate() {
		return previousEndDate;
	}

	public LocalDate newEndDate() {
		return newEndDate;
	}

	public LocalDate renewedOn() {
		return renewedOn;
	}

}
