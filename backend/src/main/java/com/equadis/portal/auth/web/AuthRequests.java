package com.equadis.portal.auth.web;

import com.equadis.portal.auth.application.AuthService;
import com.equadis.portal.auth.domain.UserAccount;
import com.equadis.portal.retailer.domain.StoreType;
import com.equadis.portal.shared.actor.ActorRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request bodies of /api/auth. */
final class AuthRequests {

	private AuthRequests() {
	}

	record Login(@NotBlank String username, @NotBlank String password) {
	}

	/** {@code persona}: ADMIN, SUPPLIER:&lt;id&gt; or RETAILER:&lt;id&gt;. */
	record ActAs(@NotBlank String persona) {
	}

	/** Sign up as a SUPPLIER (fill {@code supplier}) or a RETAILER (fill {@code retailer}). */
	record SignUp(
			@NotBlank @Pattern(regexp = UserAccount.USERNAME_REGEX,
					message = "must be 3 to 50 letters, digits, '.', '_' or '-'") String username,
			@NotBlank @Size(min = AuthService.MIN_PASSWORD_LENGTH, max = AuthService.MAX_PASSWORD_LENGTH) String password,
			@NotNull ActorRole role,
			@Valid SupplierDetails supplier,
			@Valid RetailerDetails retailer) {
	}

	record SupplierDetails(
			@NotBlank @Size(max = 120) String companyName,
			@NotBlank @Size(max = 120) String contactName,
			@NotBlank @Email @Size(max = 254) String contactEmail,
			@NotBlank String contactPhone,
			@NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be an ISO 3166-1 alpha-2 code") String countryCode) {
	}

	/** BR-02 (which location fields each store type needs) is checked by the domain. */
	record RetailerDetails(
			@NotBlank @Size(max = 120) String storeName,
			@NotNull StoreType storeType,
			@NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be an ISO 3166-1 alpha-2 code") String countryCode,
			@Size(max = 120) String city,
			@Size(max = 200) String address,
			@Size(max = 255) String website,
			@NotBlank @Email @Size(max = 254) String contactEmail) {
	}

}
