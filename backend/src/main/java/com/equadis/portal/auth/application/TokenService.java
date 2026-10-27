package com.equadis.portal.auth.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.shared.actor.TokenClaims;
import com.equadis.portal.shared.security.JwtProperties;

/** Issues signed JWTs (HS256) describing who is signed in and which persona they act as. */
@Component
public class TokenService {

	private final JwtEncoder encoder;

	private final JwtProperties properties;

	private final Clock clock;

	TokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
		this.encoder = encoder;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * @param username the account that signed in (the token subject)
	 * @param actor the persona the token acts as
	 * @param displayName company name for supplier and retailer personas, null for ADMIN
	 * @param canActAs whether the account is an administrator allowed to switch persona
	 */
	public AuthSession issue(String username, CurrentActor actor, String displayName, boolean canActAs) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(properties.ttl());
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(username)
			.issuedAt(now)
			.expiresAt(expiresAt)
			.claim(TokenClaims.ROLE, actor.role().name())
			.claim(TokenClaims.CAN_ACT_AS, canActAs);
		if (actor.id() != null) {
			claims.claim(TokenClaims.PARTY_ID, actor.id());
		}
		if (displayName != null) {
			claims.claim(TokenClaims.NAME, displayName);
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
		return new AuthSession(token, expiresAt, username, actor.role(), actor.id(), displayName, canActAs);
	}

}
