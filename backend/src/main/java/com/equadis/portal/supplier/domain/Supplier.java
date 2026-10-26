package com.equadis.portal.supplier.domain;

import java.util.Objects;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.shared.domain.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Supplier aggregate root. Its own invariants are checked here; the company name
 * uniqueness rule (BR-01) needs the other suppliers, so the application service checks it.
 */
@Entity
@Table(name = "supplier")
public class Supplier {

	public static final int MAX_COMPANY_NAME_LENGTH = 120;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "company_name", nullable = false, length = MAX_COMPANY_NAME_LENGTH)
	private String companyName;

	@Embedded
	private ContactInfo contact;

	/** Stored as the plain ISO code so it can be searched and sorted. */
	@Column(name = "country_code", nullable = false, length = 2)
	private String countryCode;

	@Version
	private long version;

	protected Supplier() {
	}

	private Supplier(String companyName, ContactInfo contact, CountryCode countryCode) {
		apply(companyName, contact, countryCode);
	}

	public static Supplier register(String companyName, ContactInfo contact, CountryCode countryCode) {
		return new Supplier(companyName, contact, countryCode);
	}

	public void update(String companyName, ContactInfo contact, CountryCode countryCode) {
		apply(companyName, contact, countryCode);
	}

	/** Company name as it is compared for uniqueness (BR-01). */
	public static String normalizeName(String companyName) {
		return companyName == null ? "" : companyName.trim();
	}

	private void apply(String companyName, ContactInfo contact, CountryCode countryCode) {
		String name = normalizeName(companyName);
		if (name.isEmpty() || name.length() > MAX_COMPANY_NAME_LENGTH) {
			throw new BusinessException(ErrorCode.INVALID_SUPPLIER,
					"Company name is required (max " + MAX_COMPANY_NAME_LENGTH + " characters)");
		}
		this.companyName = name;
		this.contact = Objects.requireNonNull(contact, "contact");
		this.countryCode = Objects.requireNonNull(countryCode, "countryCode").value();
	}

	public Long id() {
		return id;
	}

	public String companyName() {
		return companyName;
	}

	public ContactInfo contact() {
		return contact;
	}

	public CountryCode countryCode() {
		return CountryCode.of(countryCode);
	}

}
