package com.equadis.portal.shared.actor;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/** Injects a {@link CurrentActor} parameter into controller methods, read from the verified JWT. */
public class CurrentActorArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return CurrentActor.class.equals(parameter.getParameterType());
	}

	@Override
	public CurrentActor resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token) {
			return TokenClaims.actorOf(token.getToken());
		}
		throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Sign in first");
	}

}
