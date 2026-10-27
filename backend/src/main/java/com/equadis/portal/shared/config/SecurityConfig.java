package com.equadis.portal.shared.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.equadis.portal.shared.security.JwtProperties;
import com.equadis.portal.shared.security.ProblemDetailSecurityHandler;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Stateless JWT security: every /api call needs a valid bearer token, except signing in,
 * signing up and the persona reference list. What each persona may do is checked in the
 * controllers with ActorAccess, from the CurrentActor in the token.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
		ProblemDetailSecurityHandler problems = new ProblemDetailSecurityHandler();
		return http
			// No cookies or sessions: the token travels in the Authorization header, so CSRF does not apply.
			.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(requests -> requests
				.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/signup").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/personas").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())
				.authenticationEntryPoint(problems)
				.accessDeniedHandler(problems))
			.exceptionHandling(errors -> errors.authenticationEntryPoint(problems).accessDeniedHandler(problems))
			.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		// bcrypt, with the {id} prefix so the algorithm can be upgraded later.
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	SecretKey jwtSigningKey(JwtProperties properties) {
		return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
		return decoder;
	}

}
