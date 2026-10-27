package com.equadis.portal.shared.domain;

import java.util.regex.Pattern;

/** E-mail address, trimmed. Checked loosely (one "@", a dot in the domain); the real proof is delivery. */
public record Email(String value) {

	public static final int MAX_LENGTH = 254;

	private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	public Email {
		value = value == null ? "" : value.trim();
		if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid e-mail address: '" + value + "'");
		}
	}

	public static Email of(String value) {
		return new Email(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
