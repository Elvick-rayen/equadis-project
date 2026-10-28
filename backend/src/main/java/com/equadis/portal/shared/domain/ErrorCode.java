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
	INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST),

	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
	USERNAME_TAKEN(HttpStatus.CONFLICT),
	INVALID_SIGNUP(HttpStatus.BAD_REQUEST),
	INVALID_ACTING_AS(HttpStatus.BAD_REQUEST),
	FORBIDDEN_ACTION(HttpStatus.FORBIDDEN),

	INVALID_COUNTRY_CODE(HttpStatus.BAD_REQUEST),
	INVALID_CONTACT(HttpStatus.BAD_REQUEST),

	SUPPLIER_NOT_FOUND(HttpStatus.NOT_FOUND),
	SUPPLIER_NAME_TAKEN(HttpStatus.CONFLICT),
	INVALID_SUPPLIER(HttpStatus.BAD_REQUEST),
	/** The supplier still has partnerships or offers. */
	SUPPLIER_IN_USE(HttpStatus.CONFLICT),

	RETAILER_NOT_FOUND(HttpStatus.NOT_FOUND),
	INVALID_RETAILER(HttpStatus.BAD_REQUEST),
	INVALID_LOCATION(HttpStatus.BAD_REQUEST),
	/** The retailer still has partnerships or subscriptions. */
	RETAILER_IN_USE(HttpStatus.CONFLICT),

	PARTNERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND),
	/** BR-03 */
	START_DATE_IN_PAST(HttpStatus.BAD_REQUEST),
	/** BR-03 / BR-06 */
	PARTNERSHIP_TOO_LONG(HttpStatus.BAD_REQUEST),
	/** BR-04 */
	PARTNERSHIP_OVERLAP(HttpStatus.CONFLICT),
	/** BR-06 */
	RENEWAL_NOT_ALLOWED(HttpStatus.CONFLICT),
	INVALID_RENEWAL_DATE(HttpStatus.BAD_REQUEST),
	/** BR-07 */
	TERMINATION_NOT_ALLOWED(HttpStatus.CONFLICT),
	TERMINATION_REASON_REQUIRED(HttpStatus.BAD_REQUEST),

	OFFER_NOT_FOUND(HttpStatus.NOT_FOUND),
	OFFER_REFERENCE_TAKEN(HttpStatus.CONFLICT),
	INVALID_OFFER(HttpStatus.BAD_REQUEST),
	/** BR-09 */
	INVALID_DISCOUNT(HttpStatus.BAD_REQUEST),
	/** BR-08: only drafts can be edited; the lifecycle only moves forward. */
	OFFER_NOT_EDITABLE(HttpStatus.CONFLICT),
	INVALID_OFFER_TRANSITION(HttpStatus.CONFLICT),

	SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND),
	/** BR-10 */
	OFFER_NOT_AVAILABLE(HttpStatus.CONFLICT),
	/** BR-11 */
	ALREADY_SUBSCRIBED(HttpStatus.CONFLICT),
	SUBSCRIPTION_ALREADY_CANCELLED(HttpStatus.CONFLICT);

	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus status() {
		return status;
	}

}
