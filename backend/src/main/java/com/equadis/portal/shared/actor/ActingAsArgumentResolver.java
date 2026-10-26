package com.equadis.portal.shared.actor;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Injects a {@link CurrentActor} parameter into controller methods, read from the X-Acting-As header. */
public class ActingAsArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return CurrentActor.class.equals(parameter.getParameterType());
	}

	@Override
	public CurrentActor resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		return CurrentActor.parse(webRequest.getHeader(CurrentActor.HEADER));
	}

}
