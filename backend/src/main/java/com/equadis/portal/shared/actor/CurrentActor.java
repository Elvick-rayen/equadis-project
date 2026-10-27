package com.equadis.portal.shared.actor;

import java.util.Objects;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/**
 * Who the request is acting as. {@code id} is the supplier or retailer id, and null for ADMIN.
 * Read from the verified JWT (see {@link TokenClaims}); written as a persona key
 * {@code ADMIN}, {@code SUPPLIER:12} or {@code RETAILER:3} when an administrator switches persona.
 */
public record CurrentActor(ActorRole role, Long id) {

	public CurrentActor {
		Objects.requireNonNull(role, "role");
		if ((role == ActorRole.ADMIN) != (id == null)) {
			throw new IllegalArgumentException("ADMIN has no id; SUPPLIER and RETAILER need one");
		}
	}

	public static CurrentActor admin() {
		return new CurrentActor(ActorRole.ADMIN, null);
	}

	public static CurrentActor supplier(long supplierId) {
		return new CurrentActor(ActorRole.SUPPLIER, supplierId);
	}

	public static CurrentActor retailer(long retailerId) {
		return new CurrentActor(ActorRole.RETAILER, retailerId);
	}

	/** Parses a persona key: ADMIN, SUPPLIER:&lt;id&gt; or RETAILER:&lt;id&gt;. */
	public static CurrentActor parse(String key) {
		if (key != null) {
			String[] parts = key.trim().split(":", -1);
			try {
				ActorRole role = ActorRole.valueOf(parts[0]);
				if (role == ActorRole.ADMIN && parts.length == 1) {
					return admin();
				}
				if (role != ActorRole.ADMIN && parts.length == 2) {
					return new CurrentActor(role, Long.parseLong(parts[1]));
				}
			}
			catch (IllegalArgumentException ignored) {
				// Unknown role or non-numeric id: reported below.
			}
		}
		throw new BusinessException(ErrorCode.INVALID_ACTING_AS,
				"Invalid persona '" + key + "'. Expected ADMIN, SUPPLIER:<id> or RETAILER:<id>");
	}

	public boolean isAdmin() {
		return role == ActorRole.ADMIN;
	}

	public boolean isSupplier(long supplierId) {
		return role == ActorRole.SUPPLIER && id == supplierId;
	}

	public boolean isRetailer(long retailerId) {
		return role == ActorRole.RETAILER && id == retailerId;
	}

}
