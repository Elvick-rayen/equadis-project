package com.equadis.portal.shared.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.equadis.portal.shared.domain.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes Spring Security's 401 and 403 responses in the same ProblemDetail shape as the
 * GlobalExceptionHandler, so clients always get a business {@code code}.
 */
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
			throws IOException {
		write(request, response, ErrorCode.UNAUTHENTICATED, "Sign in to call this endpoint (missing, invalid or expired token)");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
			throws IOException {
		write(request, response, ErrorCode.FORBIDDEN_ACTION, "Not allowed for this persona");
	}

	private static void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String detail)
			throws IOException {
		response.setStatus(code.status().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		// Fixed strings only, apart from the request path, which is JSON-escaped.
		response.getWriter()
			.write("{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"instance\":\"%s\",\"code\":\"%s\"}"
				.formatted(code.name(), code.status().value(), detail, escape(request.getRequestURI()), code.name()));
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

}
