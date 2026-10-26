package com.equadis.portal.shared.domain;

import org.springframework.http.HttpStatus;

/**
 * Business error codes returned to clients in the {@code code} property of a ProblemDetail.
 * The frontend translates each code, so codes are part of the API contract.
 */
public enum ErrorCode {

	VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
	INVALID_SORT(HttpStatus.BAD_REQUEST),

	ACTING_AS_REQUIRED(HttpStatus.BAD_REQUEST),
	INVALID_ACTING_AS(HttpStatus.BAD_REQUEST),
	FORBIDDEN_ACTION(HttpStatus.FORBIDDEN),

	INVALID_COUNTRY_CODE(HttpStatus.BAD_REQUEST),
	INVALID_CONTACT(HttpStatus.BAD_REQUEST),

	SUPPLIER_NOT_FOUND(HttpStatus.NOT_FOUND),
	SUPPLIER_NAME_TAKEN(HttpStatus.CONFLICT),
	INVALID_SUPPLIER(HttpStatus.BAD_REQUEST);

	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus status() {
		return status;
	}

}
