package com.equadis.portal.shared.domain;

/** A broken business rule. Mapped to a ProblemDetail carrying the {@link ErrorCode}. */
public class BusinessException extends RuntimeException {

	private final ErrorCode code;

	public BusinessException(ErrorCode code, String message) {
		super(message);
		this.code = code;
	}

	public ErrorCode code() {
		return code;
	}

}
