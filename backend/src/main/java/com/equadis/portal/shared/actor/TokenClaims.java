package com.equadis.portal.shared.actor;

import org.springframework.security.oauth2.jwt.Jwt;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/**
 * Custom claims carried by the portal's JWTs, next to the standard ones
 * (sub = username, iss, iat, exp).
 */
public final class TokenClaims {

	/** ADMIN, SUPPLIER or RETAILER: the persona the token acts as. */
	public static final String ROLE = "role";

	/** Supplier or retailer id; absent for ADMIN. */
	public static final String PARTY_ID = "party_id";

	/** Company name shown in the UI; absent for ADMIN. */
	public static final String NAME = "name";

	/** True when the signed-in account is an administrator, who may act as any persona. */
	public static final String CAN_ACT_AS = "can_act_as";

	private TokenClaims() {
	}

	public static CurrentActor actorOf(Jwt jwt) {
		try {
			ActorRole role = ActorRole.valueOf(jwt.getClaimAsString(ROLE));
			Object partyId = jwt.getClaim(PARTY_ID);
			return new CurrentActor(role, partyId instanceof Number number ? number.longValue() : null);
		}
		catch (IllegalArgumentException | NullPointerException ex) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Token does not describe a valid persona");
		}
	}

	public static boolean canActAs(Jwt jwt) {
		return Boolean.TRUE.equals(jwt.getClaimAsBoolean(CAN_ACT_AS));
	}

}
