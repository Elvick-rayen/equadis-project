package com.equadis.portal.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings ({@code portal.security.jwt.*}). Tokens are signed with HMAC-SHA256,
 * so the secret must be at least 32 bytes; set it with the JWT_SECRET environment variable.
 */
@ConfigurationProperties("portal.security.jwt")
public record JwtProperties(String secret, String issuer, Duration ttl) {

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("portal.security.jwt.secret must be at least 32 bytes");
		}
		if (issuer == null || issuer.isBlank()) {
			issuer = "equadis-portal";
		}
		if (ttl == null) {
			ttl = Duration.ofHours(8);
		}
	}

}
