package com.equadis.portal.modules.auth.application;

import java.time.Instant;

import com.equadis.portal.shared.actor.ActorRole;

/**
 * A signed-in session: the bearer token plus what the UI needs to show it.
 * {@code partyId} and {@code displayName} are the supplier or retailer acted for (null for ADMIN).
 */
public record AuthSession(String token, Instant expiresAt, String username, ActorRole role, Long partyId,
		String displayName, boolean canActAs) {

}
