package com.equadis.portal.modules.audit.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One recorded action. Immutable once written: the log is append-only. */
@Entity
@Table(name = "audit_entry")
public class AuditEntry {

	public static final int MAX_DETAILS_LENGTH = 600;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	/** The signed-in account, plus the persona when an administrator acted as someone else. */
	@Column(name = "actor", nullable = false, length = 120)
	private String actor;

	/** e.g. PARTNERSHIP_TERMINATED, OFFER_PUBLISHED */
	@Column(name = "action", nullable = false, length = 40)
	private String action;

	@Enumerated(EnumType.STRING)
	@Column(name = "subject_type", nullable = false, length = 20)
	private AuditSubject subjectType;

	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	@Column(name = "details", length = MAX_DETAILS_LENGTH)
	private String details;

	protected AuditEntry() {
	}

	private AuditEntry(Instant occurredAt, String actor, String action, AuditSubject subjectType, long subjectId,
			String details) {
		this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
		this.actor = Objects.requireNonNull(actor, "actor");
		this.action = Objects.requireNonNull(action, "action");
		this.subjectType = Objects.requireNonNull(subjectType, "subjectType");
		this.subjectId = subjectId;
		this.details = details == null || details.length() <= MAX_DETAILS_LENGTH ? details
				: details.substring(0, MAX_DETAILS_LENGTH);
	}

	public static AuditEntry record(Instant occurredAt, String actor, String action, AuditSubject subjectType,
			long subjectId, String details) {
		return new AuditEntry(occurredAt, actor, action, subjectType, subjectId, details);
	}

	public Long id() {
		return id;
	}

	public Instant occurredAt() {
		return occurredAt;
	}

	public String actor() {
		return actor;
	}

	public String action() {
		return action;
	}

	public AuditSubject subjectType() {
		return subjectType;
	}

	public long subjectId() {
		return subjectId;
	}

	public String details() {
		return details;
	}

}
