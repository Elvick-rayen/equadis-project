package com.equadis.portal.modules.supplier;

/** Details of a supplier to register, as plain values; the supplier module validates them. */
public record NewSupplier(String companyName, String contactName, String contactEmail, String contactPhone,
		String countryCode) {

}
