package com.equadis.portal.modules.auth.domain;

import java.util.Objects;
import java.util.regex.Pattern;

import com.equadis.portal.shared.actor.ActorRole;
import com.equadis.portal.shared.actor.CurrentActor;
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
 * A login. Administrators belong to no company; a supplier or retailer account belongs to
 * exactly one, and only ever acts for it. Only the password hash is stored.
 */
@Entity
@Table(name = "app_user")
public class UserAccount {

	/** Letters, digits, ".", "_" and "-", 3 to 50 characters. */
	public static final String USERNAME_REGEX = "^[A-Za-z0-9._-]{3,50}$";

	private static final Pattern USERNAME = Pattern.compile(USERNAME_REGEX);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "username", nullable = false, length = 50)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private ActorRole role;

	@Column(name = "supplier_id")
	private Long supplierId;

	@Column(name = "retailer_id")
	private Long retailerId;

	@Version
	private long version;

	protected UserAccount() {
	}

	private UserAccount(String username, String passwordHash, ActorRole role, Long supplierId, Long retailerId) {
		this.username = requireValidUsername(username);
		this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
		this.role = role;
		this.supplierId = supplierId;
		this.retailerId = retailerId;
	}

	public static UserAccount administrator(String username, String passwordHash) {
		return new UserAccount(username, passwordHash, ActorRole.ADMIN, null, null);
	}

	public static UserAccount forSupplier(String username, String passwordHash, long supplierId) {
		return new UserAccount(username, passwordHash, ActorRole.SUPPLIER, supplierId, null);
	}

	public static UserAccount forRetailer(String username, String passwordHash, long retailerId) {
		return new UserAccount(username, passwordHash, ActorRole.RETAILER, null, retailerId);
	}

	public static String requireValidUsername(String username) {
		String value = username == null ? "" : username.trim();
		if (!USERNAME.matcher(value).matches()) {
			throw new BusinessException(ErrorCode.INVALID_SIGNUP,
					"Username must be 3 to 50 letters, digits, '.', '_' or '-'");
		}
		return value;
	}

	/** The persona this account acts as when it signs in. */
	public CurrentActor actor() {
		return switch (role) {
			case ADMIN -> CurrentActor.admin();
			case SUPPLIER -> CurrentActor.supplier(supplierId);
			case RETAILER -> CurrentActor.retailer(retailerId);
		};
	}

	public Long id() {
		return id;
	}

	public String username() {
		return username;
	}

	public String passwordHash() {
		return passwordHash;
	}

	public ActorRole role() {
		return role;
	}

}
