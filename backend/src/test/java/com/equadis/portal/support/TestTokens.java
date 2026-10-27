package com.equadis.portal.support;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.equadis.portal.auth.application.TokenService;
import com.equadis.portal.shared.actor.CurrentActor;

/** Signs real JWTs for a persona, so web tests go through the same security as the app. */
@TestComponent
public class TestTokens {

	private final TokenService tokens;

	public TestTokens(TokenService tokens) {
		this.tokens = tokens;
	}

	public String tokenFor(CurrentActor actor) {
		return tokens.issue("test-" + actor.role().name().toLowerCase(), actor, null, actor.isAdmin()).token();
	}

	public MockHttpServletRequestBuilder as(CurrentActor actor, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(actor));
	}

}
