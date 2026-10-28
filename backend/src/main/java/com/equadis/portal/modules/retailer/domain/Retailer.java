package com.equadis.portal.modules.retailer.domain;

import java.util.Objects;

import com.equadis.portal.modules.retailer.StoreType;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.Email;
import com.equadis.portal.shared.domain.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Retailer aggregate root. Its location must suit its store type (BR-02, see {@link Location}). */
@Entity
@Table(name = "retailer")
public class Retailer {

	public static final int MAX_STORE_NAME_LENGTH = 120;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "store_name", nullable = false, length = MAX_STORE_NAME_LENGTH)
	private String storeName;

	@Enumerated(EnumType.STRING)
	@Column(name = "store_type", nullable = false, length = 10)
	private StoreType storeType;

	@Embedded
	private Location location;

	@Column(name = "contact_email", nullable = false, length = Email.MAX_LENGTH)
	private String contactEmail;

	@Version
	private long version;

	protected Retailer() {
	}

	private Retailer(String storeName, StoreType storeType, Location location, Email contactEmail) {
		apply(storeName, storeType, location, contactEmail);
	}

	/** The location must have been built for the same store type, via {@link Location#forStoreType}. */
	public static Retailer register(String storeName, StoreType storeType, Location location, Email contactEmail) {
		return new Retailer(storeName, storeType, location, contactEmail);
	}

	/** Replaces all details. Same rules as {@link #register}. */
	public void update(String storeName, StoreType storeType, Location location, Email contactEmail) {
		apply(storeName, storeType, location, contactEmail);
	}

	/** The single place where a retailer's details are checked and set, for both register and update. */
	private void apply(String storeName, StoreType storeType, Location location, Email contactEmail) {
		String name = storeName == null ? "" : storeName.trim();
		if (name.isEmpty() || name.length() > MAX_STORE_NAME_LENGTH) {
			throw new BusinessException(ErrorCode.INVALID_RETAILER,
					"Store name is required (max " + MAX_STORE_NAME_LENGTH + " characters)");
		}
		this.storeName = name;
		this.storeType = Objects.requireNonNull(storeType, "storeType");
		this.location = Objects.requireNonNull(location, "location");
		this.contactEmail = Objects.requireNonNull(contactEmail, "contactEmail").value();
	}

	public Long id() {
		return id;
	}

	public String storeName() {
		return storeName;
	}

	public StoreType storeType() {
		return storeType;
	}

	public Location location() {
		return location;
	}

	public Email contactEmail() {
		return Email.of(contactEmail);
	}

}
