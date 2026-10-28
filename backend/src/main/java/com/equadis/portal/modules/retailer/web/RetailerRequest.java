package com.equadis.portal.modules.retailer.web;

import com.equadis.portal.modules.retailer.StoreType;
import com.equadis.portal.modules.retailer.domain.Retailer;
import com.equadis.portal.shared.domain.Email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST and PUT /api/retailers. Checks the shape of the input; which location fields
 * a store type needs (BR-02) is checked by the domain, in {@code Location.forStoreType}.
 */
public record RetailerRequest(
		@NotBlank @Size(max = Retailer.MAX_STORE_NAME_LENGTH) String storeName,
		@NotNull StoreType storeType,
		@NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be an ISO 3166-1 alpha-2 code") String countryCode,
		@Size(max = 120) String city,
		@Size(max = 200) String address,
		@Size(max = 255) String website,
		@NotBlank @jakarta.validation.constraints.Email @Size(max = Email.MAX_LENGTH) String contactEmail) {

}
