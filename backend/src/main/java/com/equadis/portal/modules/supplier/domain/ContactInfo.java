package com.equadis.portal.modules.supplier.domain;

import java.util.regex.Pattern;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Supplier contact: name, e-mail and phone. Values are trimmed; all three are required. */
@Embeddable
public record ContactInfo(
		@Column(name = "contact_name", nullable = false, length = 120) String name,
		@Column(name = "contact_email", nullable = false, length = 254) String email,
		@Column(name = "contact_phone", nullable = false, length = 30) String phone) {

	public static final int MAX_NAME_LENGTH = 120;

	public static final int MAX_EMAIL_LENGTH = 254;

	/** Digits, spaces, "+", "-", "." and parentheses, 6 to 20 characters. */
	public static final String PHONE_REGEX = "^\\+?[0-9 ().-]{6,20}$";

	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	private static final Pattern PHONE = Pattern.compile(PHONE_REGEX);

	public ContactInfo {
		name = trim(name);
		email = trim(email);
		phone = trim(phone);
		if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
			throw invalid("Contact name is required (max " + MAX_NAME_LENGTH + " characters)");
		}
		if (email.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(email).matches()) {
			throw invalid("Contact e-mail is not valid");
		}
		if (!PHONE.matcher(phone).matches()) {
			throw invalid("Contact phone is not valid");
		}
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	private static BusinessException invalid(String message) {
		return new BusinessException(ErrorCode.INVALID_CONTACT, message);
	}

}
