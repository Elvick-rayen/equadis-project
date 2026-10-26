package com.equadis.portal.supplier.web;

import com.equadis.portal.supplier.domain.ContactInfo;
import com.equadis.portal.supplier.domain.Supplier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST and PUT /api/suppliers. */
public record SupplierRequest(
		@NotBlank @Size(max = Supplier.MAX_COMPANY_NAME_LENGTH) String companyName,
		@NotNull @Valid Contact contact,
		@NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be an ISO 3166-1 alpha-2 code") String countryCode) {

	public record Contact(
			@NotBlank @Size(max = ContactInfo.MAX_NAME_LENGTH) String name,
			@NotBlank @Email @Size(max = ContactInfo.MAX_EMAIL_LENGTH) String email,
			@NotBlank @Pattern(regexp = ContactInfo.PHONE_REGEX, message = "must be a valid phone number") String phone) {
	}

}
